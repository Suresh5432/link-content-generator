package com.linklens.userservice.digitalfootprint.messaging;

import com.linklens.userservice.digitalfootprint.entity.PrivacyEventOutboxEvent;
import com.linklens.userservice.digitalfootprint.repo.PrivacyEventOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrivacyEventOutboxPublisherTest {

    @Mock PrivacyEventOutboxRepository repository;
    @Mock RabbitTemplate rabbitTemplate;
    @InjectMocks PrivacyEventOutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        org.springframework.test.util.ReflectionTestUtils.setField(publisher, "confirmTimeoutSeconds", 2L);
        org.springframework.test.util.ReflectionTestUtils.setField(publisher, "batchSize", 20);
    }

    private PrivacyEventOutboxEvent pendingEvent() {
        return PrivacyEventOutboxEvent.builder()
                .id(java.util.UUID.randomUUID())
                .exchange("link.events")
                .routingKey("privacy.request.status.changed.v1")
                .payload("{\"eventId\":\"1\"}")
                .status("PENDING")
                .nextAttemptAt(Instant.now().minusSeconds(1))
                .build();
    }

    @Test
    void confirmedEventIsMarkedPublished() {
        PrivacyEventOutboxEvent event = pendingEvent();
        when(repository.findPendingBatch(any(Instant.class), any(Pageable.class))).thenReturn(List.of(event));
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));

        publisher.publishBatch();

        assertEquals("PUBLISHED", event.getStatus());
        assertNotNull(event.getPublishedAt());
        ArgumentCaptor<Message> message = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(eq("link.events"), eq("privacy.request.status.changed.v1"),
                message.capture(), any(CorrelationData.class));
        assertTrue(new String(message.getValue().getBody()).contains("eventId"));
        verify(repository).save(event);
    }

    @Test
    void brokerFailureSchedulesRetryAndDoesNotClaimDelivery() {
        PrivacyEventOutboxEvent event = pendingEvent();
        when(repository.findPendingBatch(any(Instant.class), any(Pageable.class))).thenReturn(List.of(event));
        doThrow(new RuntimeException("broker offline")).when(rabbitTemplate)
                .send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));

        publisher.publishBatch();

        assertEquals("PENDING", event.getStatus());
        assertEquals(1, event.getAttempts());
        assertTrue(event.getNextAttemptAt().isAfter(Instant.now()));
        assertTrue(event.getLastError().contains("broker offline"));
        verify(repository).save(event);
    }
}
