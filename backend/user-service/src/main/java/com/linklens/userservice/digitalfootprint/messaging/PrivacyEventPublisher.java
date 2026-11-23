package com.linklens.userservice.digitalfootprint.messaging;

import com.linkcontext.contracts.EventRoutingKey;
import com.linkcontext.contracts.events.PrivacyRequestReminderEvent;
import com.linkcontext.contracts.events.PrivacyRequestStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PrivacyEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${digital-footprint.events.exchange:link.events}")
    private String exchange;

    public void statusChanged(UUID requestId, String userId, String requestType, String previous, String next) {
        var event = new PrivacyRequestStatusChangedEvent(UUID.randomUUID(), requestId, userId, requestType, previous, next, Instant.now(), 1);
        send(EventRoutingKey.PRIVACY_REQUEST_STATUS_CHANGED, event);
    }

    public void reminder(UUID requestId, String userId, String requestType, String status) {
        var event = new PrivacyRequestReminderEvent(UUID.randomUUID(), requestId, userId, requestType, status, Instant.now(), 1);
        send(EventRoutingKey.PRIVACY_REQUEST_REMINDER, event);
    }

    private void send(String routingKey, Object event) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
        } catch (Exception e) {
            log.warn("Could not publish {} event; privacy data was saved and will remain available in the dashboard: {}", routingKey, e.getMessage());
        }
    }
}
