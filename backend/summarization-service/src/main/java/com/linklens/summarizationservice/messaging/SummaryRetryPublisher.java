package com.linklens.summarizationservice.messaging;

import com.linklens.summarizationservice.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class SummaryRetryPublisher {

    private static final String RETRY_COUNT_HEADER="x-retry-count";

    private static final int MAX_RETRIES=3;

    private final RabbitTemplate rabbitTemplate;

    public void retry(Message original) throws Exception {
        int retryCount = getRetryCount(original);

        if (retryCount >= MAX_RETRIES) {
            log.warn("Summary event exceeded retry limit; sending to DLQ");
            sendToDlq(original, "Maximum retry attempts exceeded");
            return;
        }

        Message retryMessage= MessageBuilder
                .fromMessage(original)
                .setHeader(RETRY_COUNT_HEADER,retryCount+1)
                .build();

        publishConfirmed(
                RabbitMQConfig.RETRY_EXCHANGE,
                RabbitMQConfig.RETRY_ROUTING_KEY,
                retryMessage
        );
        log.info("Published summary event to retry queue (attempt {})", retryCount + 1);
    }

    public void sendToDlq(Message original, String reason) throws Exception {

        Message deadLetterMassage=MessageBuilder
                .fromMessage(original)
                .setHeader("x-dead-letter-reason",reason)
                .build();

        publishConfirmed(
                RabbitMQConfig.DLQ_EXCHANGE,
                RabbitMQConfig.DLQ_ROUTING_KEY,
                deadLetterMassage

        );
        log.warn("Published summary event to DLQ: {}", reason);
    }

    private int getRetryCount(Message message){

        Object value=message.getMessageProperties()
                .getHeaders()
                .get(RETRY_COUNT_HEADER);

        if (value instanceof Number number) {
            return number.intValue();
        }

        return 0;
    }

    private void publishConfirmed(
            String exchange,
            String routingKey,
            Message message) throws Exception {

        String messageId=message.getMessageProperties().getMessageId();

        CorrelationData correlationData=new CorrelationData(
                messageId !=null ? messageId : UUID.randomUUID().toString()
        );

        rabbitTemplate.send(
                exchange,
                routingKey,
                message,
                correlationData
        );

        CorrelationData.Confirm confirm=correlationData
                .getFuture()
                .get(5, TimeUnit.SECONDS);

        if (!confirm.ack()){
            log.error("RabbitMQ did not confirm summary retry/DLQ message {}", messageId);
            throw new IllegalStateException(
                    "RabbitMQ did not confirm the message"
            );
        }

        if (correlationData.getReturned()!=null){
            log.error("RabbitMQ returned unroutable summary retry/DLQ message {}", messageId);
            throw new IllegalStateException(
                    "RabbitMQ returned an unroutable message"
            );
        }
    }
}
