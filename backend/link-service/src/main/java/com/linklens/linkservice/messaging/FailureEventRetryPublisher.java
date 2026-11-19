package com.linklens.linkservice.messaging;


import com.linklens.linkservice.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.health.actuate.endpoint.StatusAggregator;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class FailureEventRetryPublisher {

    private static final String RETRY_HEADER=
            "x-consumer-retry-count";

    private static final long CONFIRM_TIMEOUT_SECONDS = 5;

    private final RabbitTemplate rabbitTemplate;


    public void retry(
            Message originalMessage,
            String routingKey,
            String retryQueue,
            String dlqRoutingKey) {

        Object header = originalMessage.getMessageProperties()
                .getHeaders()
                .get(RETRY_HEADER);

        int retries = header instanceof Number
                ? ((Number) header).intValue()
                : 0;

        boolean exhausted =
                retries >= RabbitMQConfig.MAX_FAILURE_RETRIES;

        String exchange = exhausted
                ? RabbitMQConfig.FAILURE_DLQ_EXCHANGE
                : RabbitMQConfig.FAILURE_RETRY_EXCHANGE;

        String destinationRoutingKey = exhausted
                ? dlqRoutingKey
                : routingKey;

        Message outgoingMessage = exhausted
                ? originalMessage
                : MessageBuilder.fromMessage(originalMessage)
                .setHeader(RETRY_HEADER, retries + 1)
                .build();

        publishAndConfirm(
                exchange,
                destinationRoutingKey,
                outgoingMessage
        );

        if (exhausted) {
            log.error("Message forwarded to DLQ after {} retries", retries);
        } else {
            log.warn("Message forwarded for retry {}/{}",
                    retries + 1,
                    RabbitMQConfig.MAX_FAILURE_RETRIES);
        }
    }

    private void publishAndConfirm(
            String exchange,
            String routingKey,
            Message message) {

        CorrelationData correlationData =
                new CorrelationData(UUID.randomUUID().toString());

        try {
            rabbitTemplate.send(
                    exchange,
                    routingKey,
                    message,
                    correlationData
            );

            CorrelationData.Confirm confirm =
                    correlationData.getFuture().get(
                            CONFIRM_TIMEOUT_SECONDS,
                            TimeUnit.SECONDS
                    );

            if (!confirm.ack()) {
                throw new IllegalStateException(
                        "RabbitMQ rejected message: " + confirm.reason()
                );
            }

            if (correlationData.getReturned() != null) {
                throw new IllegalStateException(
                        "RabbitMQ returned an unroutable message"
                );
            }

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Could not confirm message forwarding",
                    exception
            );
        }
    }
}


