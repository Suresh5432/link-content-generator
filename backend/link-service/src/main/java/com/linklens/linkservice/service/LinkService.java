package com.linklens.linkservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
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

        if(idempotencyKey!=null){
            var existingLink=linkRepository
                    .findByUserIdAndIdempotencyKey(userId, idempotencyKey);

            if(existingLink.isPresent()){
                Link existing=existingLink.get();

                if(!existing.getNormalizedUrl().equals(normalizedUrl)){
                    throw new IdempotencyConflictException(
                            "Idempotency key was already used for another URL");
                }
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
        LinkSubmittedEvent event =new LinkSubmittedEvent(
                UUID.randomUUID(),
                link.getId(),
                userId,
                normalizedUrl,
                Instant.now(),
                1
        );

        String payload=serializedEvent(event);
        OutboxEvent outboxEvent=OutboxEvent.builder()
                .id(event.eventId())
                .aggregateType("LINK")
                .aggregateId(link.getId())
                .eventType(EventRoutingKey.LINK_SUBMITTED)
                .payload(payload)
                .build();
        Link savedLink=linkRepository.save(link);

        outboxRepository.save(outboxEvent);

        return mapToResponse(savedLink);
    }

    @Transactional(readOnly=true)
    public LinkResponse getLink(UUID linkId, String userId){
        validateUserId(userId);
        Link link=linkRepository.findByIdAndUserIdAndDeletedAtIsNull(linkId,userId)
                .orElseThrow(()->
                        new ResourceNotFoundException("Link not found"));

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
                .orElseThrow(()->
                        new ResourceNotFoundException("Link not found"));
        link.softDelete();
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


}
