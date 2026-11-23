package com.linklens.extractionservice.messaging;


import com.linkcontext.contracts.EventRoutingKey;
import com.linkcontext.contracts.events.ExtractionFailedEvent;
import com.linkcontext.contracts.events.LinkSubmittedEvent;
import com.linklens.extractionservice.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class ExtractionRetryPublisher {

    private static final String RETRY_COUNT_HEADER="x-retry-count";

    private static final int MAX_RETRIES=3;

    private static final long CONFIRM_TIMEOUT_SECONDS=5;

    private final RabbitTemplate rabbitTemplate;

    public void retry(Message originalMessage) throws Exception{

        int retryCount=getRetryCount(originalMessage);

        if(retryCount>=MAX_RETRIES){
            publishToDlq(originalMessage, "Maximum retry attempts exceeded");
            return;
        }

        Message retryMessage= MessageBuilder
                .fromMessage(originalMessage)
                .setHeader(RETRY_COUNT_HEADER, retryCount+1)
                .setHeader("x-last-failure", "Retryable extraction failure")
                .build();
        publishConfirmed(
                RabbitMQConfig.RETRY_EXCHANGE,
                RabbitMQConfig.RETRY_ROUTING_KEY,
                retryMessage
        );
    }

    public boolean retriesExhausted(Message originalMessage) {
        return getRetryCount(originalMessage) >= MAX_RETRIES;
    }

    /** Publish a terminal failure so link and notification services update user-facing status. */
    public void publishFailureEvent(LinkSubmittedEvent submitted, String failureCode) throws Exception {
        UUID eventId = UUID.nameUUIDFromBytes((submitted.eventId() + ":" + failureCode).getBytes(StandardCharsets.UTF_8));
        ExtractionFailedEvent event = new ExtractionFailedEvent(eventId, submitted.linkId(), submitted.userId(), failureCode, false, Instant.now(), 1);
        Message failureMessage = rabbitTemplate.getMessageConverter().toMessage(event, new MessageProperties());
        publishConfirmed(RabbitMQConfig.LINK_EXCHANGE, EventRoutingKey.EXTRACTION_FAILED, failureMessage);
    }

    public void publishToDlq(Message originalMessage, String reason) throws Exception{
        Message deadLetterMessage=MessageBuilder
                .fromMessage(originalMessage)
                .setHeader("x-dead-letter-reason",reason)
                .build();
        publishConfirmed(
            RabbitMQConfig.DLQ_EXCHANGE,
            RabbitMQConfig.DLQ_ROUTING_KEY,
            deadLetterMessage
        );
    }

    private int getRetryCount(Message message){
        Object value=message.getMessageProperties()
                .getHeaders()
                .get(RETRY_COUNT_HEADER);

        if (value instanceof Number number){
            return number.intValue();
        }

        return 0;
    }

    private void publishConfirmed(String exchange,
                                  String routingKey,
                                  Message message) throws Exception{
        String messageId=message.getMessageProperties()
                .getMessageId();

        CorrelationData correlationData=
                new CorrelationData(
                        messageId!=null ? messageId : UUID.randomUUID().toString());

        rabbitTemplate.send(exchange,
                routingKey,
                message,
                correlationData);

        CorrelationData.Confirm confirm=correlationData
                .getFuture()
                .get(CONFIRM_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        if (!confirm.ack()){
            throw new IllegalStateException(
                    "RabbitMQ did not confirm republished message"
            );
        }
        if (correlationData.getReturned()!=null){
            throw new IllegalStateException(
                    "Republished message was returned as unroutable"
            );
        }
    }

}
