package com.linklens.summarizationservice.messaging;

import com.linkcontext.contracts.events.ContentExtractedEvent;
import com.linklens.summarizationservice.config.RabbitMQConfig;
import com.linklens.summarizationservice.service.SummaryEventProcessingService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class ContentExtractedConsumer {

    private final ObjectMapper objectMapper;

    private final SummaryEventProcessingService processingService;

    private final SummaryRetryPublisher retryPublisher;

    @RabbitListener(
            queues = RabbitMQConfig.CONTENT_EXTRACTED_QUEUE
    )
    public void consume(Message message, Channel channel) throws IOException {

        long deliveryTag=message
                .getMessageProperties()
                .getDeliveryTag();

        ContentExtractedEvent event;

        try{
            event=objectMapper.readValue(
                    message.getBody(),
                    ContentExtractedEvent.class
            );
        }
        catch (Exception e){
            log.error("Unable to deserialize content extracted event; sending to DLQ", e);
            publishDlqAndAck(
                    message,
                    channel,
                    deliveryTag,
                    "Malformed content extracted event"
            );

            return;
        }

        try {
            log.info("Processing content extracted event {} for link {}", event.eventId(), event.linkId());
            processingService.process(event);

            channel.basicAck(deliveryTag,false);
            log.debug("Acknowledged content extracted event {}", event.eventId());
        }
        catch (Exception e){
            log.error("Summary processing failed for event {}; attempting retry", event.eventId(), e);
            retryAndAck(message, event, channel, deliveryTag);
        }
    }

    private void retryAndAck(
            Message message,
            ContentExtractedEvent event,
            Channel channel,
            long deliveryTag) throws IOException {

        try {
            retryPublisher.retry(message);
            if (retryPublisher.retriesExhausted(message)) {
                processingService.recordTerminalFailure(event);
            }
            channel.basicAck(deliveryTag,false);
            log.info("Republished failed summary event for retry");
        }
        catch (Exception e){
            log.error("Retry publishing failed; trying confirmed DLQ fallback", e);
            try {
                retryPublisher.sendToDlq(message, "Retry forwarding failed");
                processingService.recordTerminalFailure(event);
                channel.basicAck(deliveryTag, false);
            } catch (Exception deadLetterFailure) {
                deadLetterFailure.addSuppressed(e);
                log.error("Retry and DLQ forwarding failed; requeuing summary event", deadLetterFailure);
                channel.basicNack(deliveryTag, false, true);
            }
        }
    }

    private void publishDlqAndAck(
            Message message,
            Channel channel,
            long deliveryTag,
            String reason) throws IOException {

        try {
            retryPublisher.sendToDlq(message,reason);
            channel.basicAck(deliveryTag,false);
            log.warn("Sent malformed summary event to DLQ: {}", reason);
        }
        catch (Exception e){
            log.error("DLQ publishing failed; requeuing summary event", e);
            channel.basicNack(deliveryTag,false,true);
        }
    }
}
