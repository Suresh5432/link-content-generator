package com.linklens.linkservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkcontext.contracts.EventRoutingKey;
import com.linkcontext.contracts.events.LinkSubmittedEvent;
import com.linklens.linkservice.dto.CreateLinkRequest;
import com.linklens.linkservice.dto.LinkResponse;
import com.linklens.linkservice.entity.Link;
import com.linklens.linkservice.entity.OutboxEvent;
import com.linklens.linkservice.enums.LinkStatus;
import com.linklens.linkservice.exception.IdempotencyConflictException;
import com.linklens.linkservice.repo.LinkRepository;
import com.linklens.linkservice.repo.OutboxRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
