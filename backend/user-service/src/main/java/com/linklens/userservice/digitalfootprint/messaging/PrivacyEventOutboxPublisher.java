package com.linklens.userservice.digitalfootprint.messaging;

import com.linklens.userservice.digitalfootprint.entity.PrivacyEventOutboxEvent;
import com.linklens.userservice.digitalfootprint.repo.PrivacyEventOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class PrivacyEventOutboxPublisher {

    private final PrivacyEventOutboxRepository repository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${digital-footprint.outbox.confirm-timeout-seconds:5}")
    private long confirmTimeoutSeconds;

    @Value("${digital-footprint.outbox.batch-size:20}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${digital-footprint.outbox.fixed-delay-ms:1000}",
            initialDelayString = "${digital-footprint.outbox.initial-delay-ms:5000}")
    @Transactional
    public void publishBatch() {
        List<PrivacyEventOutboxEvent> events = repository.findPendingBatch(
                Instant.now(), PageRequest.of(0, Math.max(1, batchSize)));
        for (PrivacyEventOutboxEvent event : events) {
            publishOne(event);
            repository.save(event);
        }
    }

    private void publishOne(PrivacyEventOutboxEvent event) {
        CorrelationData correlationData = new CorrelationData(event.getId().toString());
        try {
            Message message = MessageBuilder
                    .withBody(event.getPayload().getBytes(StandardCharsets.UTF_8))
                    .setContentType("application/json")
                    .setMessageId(event.getId().toString())
                    .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                    .build();
            rabbitTemplate.send(event.getExchange(), event.getRoutingKey(), message, correlationData);
            CorrelationData.Confirm confirm = correlationData.getFuture()
                    .get(Math.max(1, confirmTimeoutSeconds), TimeUnit.SECONDS);
            if (confirm == null || !confirm.ack()) {
                throw new IllegalStateException("Broker did not confirm the privacy event");
            }
            if (correlationData.getReturned() != null) {
                throw new IllegalStateException("Privacy event was unroutable and returned");
            }
            event.setStatus("PUBLISHED");
            event.setPublishedAt(Instant.now());
            event.setLastError(null);
            log.info("Published {} privacy event {}", event.getRoutingKey(), event.getId());
        } catch (Exception exception) {
            String message = exception.getMessage() == null
                    ? exception.getClass().getSimpleName()
                    : exception.getMessage();
            event.setLastError(message.substring(0, Math.min(1000, message.length())));
            event.setAttempts(event.getAttempts() + 1);
            long backoffSeconds = Math.min(300, 1L << Math.min(8, event.getAttempts()));
            event.setNextAttemptAt(Instant.now().plusSeconds(backoffSeconds));
            if (event.getAttempts() >= 10) {
                event.setStatus("FAILED");
            }
            log.warn("Privacy event {} publish attempt {} failed: {}", event.getId(), event.getAttempts(), message);
        }
    }
}
