package com.linklens.linkservice.messaging;

import com.linklens.linkservice.config.RabbitMQConfig;
import com.linklens.linkservice.service.LinkFailureService;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FailureEventRetryFallbackTest {

    @Test
    void extractionFailureForwardingUsesRetryQueueAndDlqFallbackBeforeAck() throws IOException {
        ObjectMapper mapper = mock(ObjectMapper.class);
        LinkFailureService failureService = mock(LinkFailureService.class);
        FailureEventRetryPublisher publisher = mock(FailureEventRetryPublisher.class);
        Channel channel = mock(Channel.class);
        Message message = message(15);
        when(mapper.readValue(message.getBody(), com.linkcontext.contracts.events.ExtractionFailedEvent.class))
                .thenThrow(new IllegalArgumentException("invalid payload"));
        doThrow(new IllegalStateException("retry exchange unavailable"))
                .when(publisher).retry(message,
                        RabbitMQConfig.EXTRACTION_FAILED_ROUTING_KEY,
                        RabbitMQConfig.EXTRACTION_FAILURE_RETRY_QUEUE,
                        RabbitMQConfig.EXTRACTION_FAILED_ROUTING_KEY);

        new ExtractionFailedConsumer(mapper, failureService, publisher).consume(message, channel, 15);

        verify(publisher).deadLetter(message, RabbitMQConfig.EXTRACTION_FAILED_ROUTING_KEY,
                "Retry forwarding failed");
        verify(channel).basicAck(15, false);
        verify(channel, never()).basicNack(15, false, true);
    }

    @Test
    void summarizationFailureForwardingUsesDedicatedRetryQueueAndDlqFallbackBeforeAck() throws IOException {
        ObjectMapper mapper = mock(ObjectMapper.class);
        LinkFailureService failureService = mock(LinkFailureService.class);
        FailureEventRetryPublisher publisher = mock(FailureEventRetryPublisher.class);
        Channel channel = mock(Channel.class);
        Message message = message(16);
        when(mapper.readValue(message.getBody(), com.linkcontext.contracts.events.SummarizationFailedEvent.class))
                .thenThrow(new IllegalArgumentException("invalid payload"));
        doThrow(new IllegalStateException("retry exchange unavailable"))
                .when(publisher).retry(message,
                        RabbitMQConfig.SUMMARIZATION_FAILED_ROUTING_KEY,
                        RabbitMQConfig.SUMMARIZATION_FAILURE_RETRY_QUEUE,
                        RabbitMQConfig.SUMMARIZATION_FAILED_ROUTING_KEY);

        new SummarizationFailedConsumer(mapper, failureService, publisher).consume(message, channel, 16);

        verify(publisher).deadLetter(message, RabbitMQConfig.SUMMARIZATION_FAILED_ROUTING_KEY,
                "Retry forwarding failed");
        verify(channel).basicAck(16, false);
        verify(channel, never()).basicNack(16, false, true);
    }

    private static Message message(long deliveryTag) {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(deliveryTag);
        return new Message("{}".getBytes(), properties);
    }
}
