package com.linklens.linkservice.messaging;

import com.linkcontext.contracts.events.ExtractionFailedEvent;
import com.linklens.linkservice.config.RabbitMQConfig;
import com.linklens.linkservice.service.LinkFailureService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExtractionFailedConsumer {

    private final ObjectMapper objectMapper;

    private final LinkFailureService linkFailureService;

    private final FailureEventRetryPublisher failureEventRetryPublisher;

    @RabbitListener(
            queues = RabbitMQConfig.EXTRACTION_FAILED_QUEUE,
            ackMode = "MANUAL")
    public void consume(
            Message message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) throws IOException {

        try{

            ExtractionFailedEvent event = objectMapper
                    .readValue(
                            message.getBody(),
                            ExtractionFailedEvent.class);

            log.info(
                    "Received extraction failure event {} for link {}",
                    event.eventId(),
                    event.linkId()
            );

            if (event.retryable()){

                log.warn(
                        "Extraction failure is retryable. link={}, code={}",
                        event.linkId(),
                        event.failureCode()
                );
            }
            else{

                linkFailureService.markExtractionFailed(
                        event.eventId(),
                        event.linkId(),
                        event.userId(),
                        event.failureCode(),
                        event.retryable());
            }
            channel.basicAck(deliveryTag, false);
        }
        catch (Exception e){

            log.error(
                    "Failed to process extraction failure event",e
            );

            failureEventRetryPublisher.retry(
                    message,
                    RabbitMQConfig.EXTRACTION_FAILED_ROUTING_KEY,
                    RabbitMQConfig.EXTRACTION_FAILURE_RETRY_QUEUE,
                    RabbitMQConfig.EXTRACTION_FAILED_ROUTING_KEY
            );

            channel.basicAck(deliveryTag, false);
        }
    }
}
