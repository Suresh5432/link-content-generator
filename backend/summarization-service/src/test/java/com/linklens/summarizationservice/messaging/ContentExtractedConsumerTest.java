package com.linklens.summarizationservice.messaging;

import com.linkcontext.contracts.events.ContentExtractedEvent;
import com.linklens.summarizationservice.service.SummaryEventProcessingService;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContentExtractedConsumerTest {

    @Test
    void exhaustedSummaryRetriesPersistFailureBeforeAcknowledging() throws Exception {
        ObjectMapper mapper = mock(ObjectMapper.class);
        SummaryEventProcessingService processingService = mock(SummaryEventProcessingService.class);
        SummaryRetryPublisher retryPublisher = mock(SummaryRetryPublisher.class);
        Channel channel = mock(Channel.class);
        Message message = message(90);
        ContentExtractedEvent event = new ContentExtractedEvent(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "user-1", "title", "https://example.org", "text/html", 10,
                Instant.now(), 1);
        when(mapper.readValue(message.getBody(), ContentExtractedEvent.class)).thenReturn(event);
        when(retryPublisher.retriesExhausted(message)).thenReturn(true);
        doThrow(new IllegalStateException("AI provider timeout"))
                .when(processingService).process(event);

        new ContentExtractedConsumer(mapper, processingService, retryPublisher).consume(message, channel);

        verify(retryPublisher).retry(message);
        verify(processingService).recordTerminalFailure(event);
        verify(channel).basicAck(90, false);
        verify(channel, never()).basicNack(90, false, true);
    }

    @Test
    void retryForwardingFailureFallsBackToDeadLetterQueueBeforeAcknowledging() throws Exception {
        ObjectMapper mapper = mock(ObjectMapper.class);
        SummaryEventProcessingService processingService = mock(SummaryEventProcessingService.class);
        SummaryRetryPublisher retryPublisher = mock(SummaryRetryPublisher.class);
        Channel channel = mock(Channel.class);
        Message message = message(91);
        ContentExtractedEvent event = new ContentExtractedEvent(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "user-1", "title", "https://example.org", "text/html", 10,
                Instant.now(), 1);
        when(mapper.readValue(message.getBody(), ContentExtractedEvent.class)).thenReturn(event);
        doThrow(new IllegalStateException("retry route unavailable"))
                .when(retryPublisher).retry(message);
        doThrow(new RuntimeException("AI provider timeout"))
                .when(processingService).process(event);

        new ContentExtractedConsumer(mapper, processingService, retryPublisher).consume(message, channel);

        verify(retryPublisher).sendToDlq(message, "Retry forwarding failed");
        verify(channel).basicAck(91, false);
        verify(channel, never()).basicNack(91, false, true);
    }

    private static Message message(long deliveryTag) {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(deliveryTag);
        return new Message("{}".getBytes(), properties);
    }
}
