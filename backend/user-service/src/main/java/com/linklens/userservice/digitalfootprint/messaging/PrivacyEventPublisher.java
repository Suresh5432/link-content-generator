package com.linklens.userservice.digitalfootprint.messaging;

import com.linkcontext.contracts.EventRoutingKey;
import com.linkcontext.contracts.events.PrivacyRequestReminderEvent;
import com.linkcontext.contracts.events.PrivacyRequestStatusChangedEvent;
import com.linklens.userservice.digitalfootprint.entity.PrivacyEventOutboxEvent;
import com.linklens.userservice.digitalfootprint.repo.PrivacyEventOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PrivacyEventPublisher {

    private final PrivacyEventOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Value("${digital-footprint.events.exchange:link.events}")
    private String exchange;

    public void statusChanged(UUID requestId, String userId, String requestType, String previous, String next) {
        var event = new PrivacyRequestStatusChangedEvent(UUID.randomUUID(), requestId, userId, requestType, previous, next, Instant.now(), 1);
        record(userId, EventRoutingKey.PRIVACY_REQUEST_STATUS_CHANGED, event);
    }

    public void reminder(UUID requestId, String userId, String requestType, String status) {
        var event = new PrivacyRequestReminderEvent(UUID.randomUUID(), requestId, userId, requestType, status, Instant.now(), 1);
        record(userId, EventRoutingKey.PRIVACY_REQUEST_REMINDER, event);
    }

    @Transactional
    public void record(String routingKey, Object event) {
        record(null, routingKey, event);
    }

    @Transactional
    public void record(String userId, String routingKey, Object event) {
        try {
            outboxRepository.save(PrivacyEventOutboxEvent.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .exchange(exchange)
                    .routingKey(routingKey)
                    .payload(objectMapper.writeValueAsString(event))
                    .build());
            log.debug("Queued {} privacy event in the outbox", routingKey);
        } catch (Exception e) {
            log.error("Could not persist {} privacy event in the outbox: {}", routingKey, e.getMessage());
            throw new IllegalStateException("Unable to record privacy event for later delivery", e);
        }
    }
}
