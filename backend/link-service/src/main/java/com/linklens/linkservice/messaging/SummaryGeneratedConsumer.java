package com.linklens.linkservice.messaging;

import com.linkcontext.contracts.events.SummaryGeneratedEvent;
import com.linklens.linkservice.config.RabbitMQConfig;
import com.linklens.linkservice.service.SummaryGeneratedService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class SummaryGeneratedConsumer {

    private static final String RETRY_HEADER="x-retry-count";

    private final ObjectMapper objectMapper;

    private final SummaryGeneratedService summaryGeneratedService;

    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(
            queues = RabbitMQConfig.SUMMARY_GENERATED_QUEUE,
            ackMode = "MANUAL"
    )
    public void consume(
            Message message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            SummaryGeneratedEvent event=objectMapper.readValue(
                    message.getBody(),
                    SummaryGeneratedEvent.class
            );

            log.info("Received summary generated event {} for link {}",
                    event.eventId(), event.linkId());

            summaryGeneratedService.process(event);

            channel.basicAck(deliveryTag, false);

            log.info("Acknowledged summary event {}", event.eventId());

        }
        catch (Exception e) {

            log.error("Failed to process summary generated event",e);

            try {

                handleFailure(message, deliveryTag,channel,e);

            }
            catch (Exception publishingException){

                log.error("Failed to forward message for retry/DLQ",publishingException);

                channel.basicNack(deliveryTag, false, true);
            }
        }
    }

    private void handleFailure(
            Message message,
            long deliveryTag,
            Channel channel,
            Exception exception) throws IOException {

        Object retryHeader=message.getMessageProperties()
                .getHeaders()
                .get(RETRY_HEADER);

        int retryCount=retryHeader instanceof Number
                ?((Number)retryHeader).intValue()
                :0;

        if (retryCount<RabbitMQConfig.MAX_RETRY_ATTEMPTS) {

            int nextAttempts=retryCount+1;

            Message retryMessage= MessageBuilder
                    .fromMessage(message)
                    .setHeader(RETRY_HEADER,nextAttempts)
                    .build();

            rabbitTemplate.send(
                    "",
                    RabbitMQConfig.SUMMARY_RETRY_QUEUE,
                    retryMessage);

            channel.basicAck(deliveryTag, false);

            log.warn(
                    "Summary event scheduled for retry. Attempts {}/{}",
                    nextAttempts,
                    RabbitMQConfig.MAX_RETRY_ATTEMPTS
            );
        }
        else{
            rabbitTemplate.send(
                    "",
                    RabbitMQConfig.SUMMARY_DLQ,
                    message
            );

            channel.basicAck(deliveryTag, false);

            log.error(
                    "Summary event moved to DLQ after {} attempts",
                    retryCount
            );
        }
    }
}
