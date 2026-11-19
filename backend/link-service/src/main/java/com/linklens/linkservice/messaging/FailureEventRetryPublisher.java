package com.linklens.linkservice.messaging;


import com.linklens.linkservice.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.health.actuate.endpoint.StatusAggregator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FailureEventRetryPublisher {

    private static final String RETRY_HEADER=
            "x-consumer-retry-count";

    private final RabbitTemplate rabbitTemplate;
    private final StatusAggregator healthStatusAggregator;

    public void retry(
            Message originalMessage,
            String routingKey,
            String retryQueue,
            String dlqRoutingKey){

        Object header= originalMessage.getMessageProperties()
                .getHeaders()
                .get(RETRY_HEADER);

        int retries=header instanceof Number
                ? ((Number) header).intValue()
                : 0;

        if (retries< RabbitMQConfig.MAX_FAILURE_RETRIES){
            Message retryMessage=MessageBuilder
                    .fromMessage(originalMessage)
                    .setHeader(RETRY_HEADER,retries+1)
                    .build();

            rabbitTemplate.send(
                    RabbitMQConfig.FAILURE_RETRY_EXCHANGE,
                    routingKey,
                    retryMessage
            );

            log.warn(
                    "Failure event scheduled for retry {}/{}",
                    retries+1,
                    RabbitMQConfig.MAX_FAILURE_RETRIES
            );
        }
        else {
            rabbitTemplate.send(
                    RabbitMQConfig.FAILURE_DLQ_EXCHANGE,
                    dlqRoutingKey,
                    originalMessage
            );

            log.error(
                    "Failure event moved to DLQ after {} retries",
                    retries
            );
        }
    }
}


