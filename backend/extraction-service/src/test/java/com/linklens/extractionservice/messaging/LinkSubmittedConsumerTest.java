package com.linklens.extractionservice.messaging;

import com.linkcontext.contracts.events.LinkSubmittedEvent;
import com.linklens.extractionservice.exception.ExtractionException;
import com.linklens.extractionservice.service.LinkSubmissionProcessor;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

class LinkSubmittedConsumerTest {

    @Test
    void retryForwardingFailureFallsBackToDlqAndAcknowledgesOnlyAfterConfirm() throws Exception {
        ObjectMapper mapper = mock(ObjectMapper.class);
        LinkSubmissionProcessor processor = mock(LinkSubmissionProcessor.class);
        ExtractionRetryPublisher retryPublisher = mock(ExtractionRetryPublisher.class);
        Channel channel = mock(Channel.class);
        Message message = message(72);
        LinkSubmittedEvent event = event();
        when(mapper.readValue(message.getBody(), LinkSubmittedEvent.class)).thenReturn(event);
        doThrow(new IllegalStateException("retry route unavailable"))
                .when(retryPublisher).retry(message);
        doThrow(new RuntimeException("temporary extraction error"))
                .when(processor).process(event);

        new LinkSubmittedConsumer(mapper, processor, retryPublisher).consume(message, channel);

        verify(retryPublisher).publishToDlq(message, "Retry forwarding failed");
        verify(channel).basicAck(72, false);
        verify(channel, never()).basicNack(72, false, true);
    }

    @Test
    void retryAndDlqForwardingFailureRequeuesOriginalMessage() throws Exception {
        ObjectMapper mapper = mock(ObjectMapper.class);
        LinkSubmissionProcessor processor = mock(LinkSubmissionProcessor.class);
        ExtractionRetryPublisher retryPublisher = mock(ExtractionRetryPublisher.class);
        Channel channel = mock(Channel.class);
        Message message = message(73);
        LinkSubmittedEvent event = event();
        when(mapper.readValue(message.getBody(), LinkSubmittedEvent.class)).thenReturn(event);
        doThrow(new IllegalStateException("retry route unavailable"))
                .when(retryPublisher).retry(message);
        doThrow(new IllegalStateException("broker unavailable"))
                .when(retryPublisher).publishToDlq(eq(message), anyString());
        doThrow(new RuntimeException("temporary extraction error"))
                .when(processor).process(event);

        assertDoesNotThrow(() -> new LinkSubmittedConsumer(mapper, processor, retryPublisher).consume(message, channel));

        verify(channel).basicNack(73, false, true);
        verify(channel, never()).basicAck(73, false);
    }

    @Test
    void permanentExtractionFailurePublishesFailureEventBeforeAcknowledging() throws Exception {
        ObjectMapper mapper = mock(ObjectMapper.class);
        LinkSubmissionProcessor processor = mock(LinkSubmissionProcessor.class);
        ExtractionRetryPublisher retryPublisher = mock(ExtractionRetryPublisher.class);
        Channel channel = mock(Channel.class);
        Message message = message(74);
        LinkSubmittedEvent event = event();
        when(mapper.readValue(message.getBody(), LinkSubmittedEvent.class)).thenReturn(event);
        doThrow(new ExtractionException("unsupported page" )).when(processor).process(event);

        new LinkSubmittedConsumer(mapper, processor, retryPublisher).consume(message, channel);

        verify(retryPublisher).publishToDlq(message, "unsupported page");
        verify(retryPublisher).publishFailureEvent(event, "EXTRACTION_FAILED");
        verify(channel).basicAck(74, false);
    }

    @Test
    void exhaustedRetriesPublishFailureEvent() throws Exception {
        ObjectMapper mapper = mock(ObjectMapper.class);
        LinkSubmissionProcessor processor = mock(LinkSubmissionProcessor.class);
        ExtractionRetryPublisher retryPublisher = mock(ExtractionRetryPublisher.class);
        Channel channel = mock(Channel.class);
        Message message = message(75);
        LinkSubmittedEvent event = event();
        when(mapper.readValue(message.getBody(), LinkSubmittedEvent.class)).thenReturn(event);
        when(retryPublisher.retriesExhausted(message)).thenReturn(true);
        doThrow(new RuntimeException("temporary extraction error")).when(processor).process(event);

        new LinkSubmittedConsumer(mapper, processor, retryPublisher).consume(message, channel);

        verify(retryPublisher).retry(message);
        verify(retryPublisher).publishFailureEvent(event, "RETRIES_EXHAUSTED");
        verify(channel).basicAck(75, false);
    }

    private static Message message(long deliveryTag) {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(deliveryTag);
        return new Message("{}".getBytes(), properties);
    }

    private static LinkSubmittedEvent event() {
        return new LinkSubmittedEvent(UUID.randomUUID(), UUID.randomUUID(), "user-1",
                "https://example.org", Instant.now(), 1);
    }
}
