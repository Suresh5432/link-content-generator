package com.linklens.linkservice.messaging;

import com.linkcontext.contracts.events.SummaryGeneratedEvent;
import com.linklens.linkservice.config.RabbitMQConfig;
import com.linklens.linkservice.service.SummaryGeneratedService;
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
@Slf4j
@RequiredArgsConstructor
public class SummaryGeneratedConsumer {

    private final ObjectMapper objectMapper;

    private final SummaryGeneratedService summaryGeneratedService;

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
        }
        catch (Exception e) {

            log.error("Failed to process summary generated event",e);

            channel.basicNack(deliveryTag, false, true);
        }
    }
}
