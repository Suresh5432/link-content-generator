package com.linklens.linkservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkcontext.contracts.EventRoutingKey;
import com.linkcontext.contracts.events.LinkSubmittedEvent;
import com.linklens.linkservice.dto.CreateLinkRequest;
import com.linklens.linkservice.dto.LinkResponse;
import com.linklens.linkservice.dto.PagedResponse;
import com.linklens.linkservice.entity.Link;
import com.linklens.linkservice.entity.OutboxEvent;
import com.linklens.linkservice.enums.LinkStatus;
import com.linklens.linkservice.exception.IdempotencyConflictException;
import com.linklens.linkservice.exception.ResourceNotFoundException;
import com.linklens.linkservice.repo.LinkRepository;
import com.linklens.linkservice.repo.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LinkService {

    private final LinkRepository linkRepository;

    private final OutboxRepository outboxRepository;

    private final UrlNormalizer urlNormalizer;

    private final ObjectMapper objectMapper;

    @Transactional
    public LinkResponse createLink(CreateLinkRequest linkRequest,
                                   String userId,
                                   String idempotencyKey) {
        validateUserId(userId);
        validateIdempotencyKey(idempotencyKey);
        String normalizedUrl=urlNormalizer.normalize(linkRequest.getUrl());
        log.debug("Creating link for user {}", userId);

        if(idempotencyKey!=null){
            var existingLink=linkRepository
                    .findByUserIdAndIdempotencyKeyAndDeletedAtIsNull(userId, idempotencyKey);

            if(existingLink.isPresent()){
                Link existing=existingLink.get();

                if(!existing.getNormalizedUrl().equals(normalizedUrl)){
                    log.warn("Idempotency conflict for user {} and link {}", userId, existing.getId());
                    throw new IdempotencyConflictException(
                            "Idempotency key was already used for another URL");
                }
                log.info("Returning existing link {} for user {}", existing.getId(), userId);
                return mapToResponse(existing);
            }
        }
        Link link=Link.builder()
                .userId(userId)
                .originalUrl(linkRequest.getUrl().trim())
                .normalizedUrl(normalizedUrl)
                .idempotencyKey(idempotencyKey)
                .status(LinkStatus.PENDING_EXTRACTION)
                .build();
        if (idempotencyKey != null) linkRepository.lockIdempotencyScope(userId, idempotencyKey);
        if (idempotencyKey != null) {
            var raced = linkRepository.findByUserIdAndIdempotencyKeyAndDeletedAtIsNull(userId, idempotencyKey);
            if (raced.isPresent()) {
                Link existing = raced.get();
                if (!existing.getNormalizedUrl().equals(normalizedUrl)) throw new IdempotencyConflictException("Idempotency key was already used for another URL");
                return mapToResponse(existing);
            }
        }
        Link savedLink=linkRepository.save(link);
        LinkSubmittedEvent event =new LinkSubmittedEvent(
                UUID.randomUUID(),
                savedLink.getId(),
                userId,
                normalizedUrl,
                Instant.now(),
                1
        );

        String payload=serializedEvent(event);
        OutboxEvent outboxEvent=OutboxEvent.builder()
                .id(event.eventId())
                .aggregateType("LINK")
                .aggregateId(savedLink.getId())
                .eventType(EventRoutingKey.LINK_SUBMITTED)
                .payload(payload)
                .build();
        outboxRepository.save(outboxEvent);

        log.info("Created link {} for user {} and queued submission event {}",
                savedLink.getId(), userId, event.eventId());

        return mapToResponse(savedLink);
    }

    @Transactional(readOnly=true)
    public LinkResponse getLink(UUID linkId, String userId){
        validateUserId(userId);
        Link link=linkRepository.findByIdAndUserIdAndDeletedAtIsNull(linkId,userId)
                .orElseThrow(()-> {
                    log.debug("Link {} not found for user {}", linkId, userId);
                    return new ResourceNotFoundException("Link not found");
                });

        log.debug("Retrieved link {} for user {}", linkId, userId);
        return mapToResponse(link);
    }

    @Transactional(readOnly=true)
    public PagedResponse<LinkResponse> getLinks(String userId, int page, int size){
        validateUserId(userId);
        if(page<0){
            throw new IllegalArgumentException("Page must be zero or greater");
        }
        if(size<1 ||size>100){
            throw new IllegalArgumentException("Size must be between 1 and 100");
        }

        PageRequest pageable=PageRequest.of(page,size, Sort.by(Sort.Direction.DESC,"createdAt"));

        Page<Link> links=linkRepository.findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId,pageable);
        log.debug("Retrieved {} links for user {} (page {}, size {})",
                links.getNumberOfElements(), userId, page, size);

        return new PagedResponse<>(
                links.getContent()
                        .stream()
                        .map(this::mapToResponse)
                        .toList(),
                links.getNumber(),
                links.getSize(),
                links.getTotalElements(),
                links.getTotalPages(),
                links.isFirst(),
                links.isLast()
        );
    }

    @Transactional
    public void deleteLink(UUID linkId, String userId){
        validateUserId(userId);
        Link link=linkRepository
                .findByIdAndUserIdAndDeletedAtIsNull(linkId,userId)
                .orElseThrow(()-> {
                    log.debug("Link {} not found for deletion by user {}", linkId, userId);
                    return new ResourceNotFoundException("Link not found");
                });
        link.softDelete();
        log.info("Soft-deleted link {} for user {}", linkId, userId);
    }

    private void validateUserId(String userId) {
        if(userId==null||userId.isBlank()){
            throw new IllegalArgumentException("Authenticated user is required");
        }
    }

    private void validateIdempotencyKey(String idempotencyKey) {
        if(idempotencyKey==null){
            return;
        }
        if(idempotencyKey.isBlank() ||
                idempotencyKey.length() >128 ||
                !idempotencyKey.matches("[A-Za-z0-9._:-]+")){
            throw new IllegalArgumentException("Invalid Idempotency-Key header");
        }
    }

    private String serializedEvent(LinkSubmittedEvent event) {
        try{
            return objectMapper.writeValueAsString(event);
        }
        catch (JsonProcessingException exception){
            log.error("Failed to serialize link submission event {}", event.eventId(), exception);
            throw new IllegalArgumentException("Unable to serialize link event",
                    exception);
        }
    }

    private LinkResponse mapToResponse(Link link) {
        return  LinkResponse.builder()
                .id(link.getId())
                .url(link.getOriginalUrl())
                .status(link.getStatus())
                .createdAt(link.getCreatedAt())
                .updatedAt(link.getUpdatedAt())
                .build();
    }

    @Transactional(readOnly=true)
    public PagedResponse<LinkResponse> searchLinks(String userId, int page, int size,
            LinkStatus status, String query, String sortBy, Sort.Direction direction) {
        validateUserId(userId);
        validatePage(page, size);
        if (!java.util.Set.of("createdAt", "updatedAt", "status").contains(sortBy)) {
            throw new IllegalArgumentException("sortBy must be createdAt, updatedAt, or status");
        }
        String term = query == null ? null : query.trim();
        if (term != null && term.length() > 2048) throw new IllegalArgumentException("Search query is too long");
        Specification<Link> spec = (root, criteria, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("userId"), userId));
            predicates.add(cb.isNull(root.get("deletedAt")));
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (term != null && !term.isEmpty()) predicates.add(cb.like(cb.lower(root.get("originalUrl")), "%" + term.toLowerCase(java.util.Locale.ROOT) + "%"));
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        Page<Link> links = linkRepository.findAll(spec, PageRequest.of(page, size, Sort.by(direction, sortBy)));
        return new PagedResponse<>(links.getContent().stream().map(this::mapToResponse).toList(), links.getNumber(), links.getSize(), links.getTotalElements(), links.getTotalPages(), links.isFirst(), links.isLast());
    }

    @Transactional
    public LinkResponse retryLink(UUID linkId, String userId) {
        validateUserId(userId);
        Link link = linkRepository.findByIdAndUserIdAndDeletedAtIsNull(linkId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Link not found"));
        if (link.getStatus() != LinkStatus.EXTRACTION_FAILED && link.getStatus() != LinkStatus.SUMMARIZATION_FAILED && link.getStatus() != LinkStatus.FAILED) {
            throw new IllegalArgumentException("Only failed links can be retried");
        }
        link.updateStatus(LinkStatus.PENDING_EXTRACTION);
        LinkSubmittedEvent event = new LinkSubmittedEvent(UUID.randomUUID(), link.getId(), userId, link.getNormalizedUrl(), Instant.now(), 1);
        outboxRepository.save(OutboxEvent.builder().id(event.eventId()).aggregateType("LINK").aggregateId(link.getId()).eventType(EventRoutingKey.LINK_SUBMITTED).payload(serializedEvent(event)).build());
        return mapToResponse(link);
    }

    private void validatePage(int page, int size) {
        if (page < 0) throw new IllegalArgumentException("Page must be zero or greater");
        if (size < 1 || size > 100) throw new IllegalArgumentException("Size must be between 1 and 100");
    }


}
