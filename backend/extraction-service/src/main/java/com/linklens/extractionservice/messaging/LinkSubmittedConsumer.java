package com.linklens.extractionservice.messaging;

import com.linkcontext.contracts.events.LinkSubmittedEvent;
import com.linklens.extractionservice.config.RabbitMQConfig;
import com.linklens.extractionservice.exception.ExtractionException;
import com.linklens.extractionservice.exception.RetryableExtractionException;
import com.linklens.extractionservice.exception.UnsafeUrlException;
import com.linklens.extractionservice.service.LinkSubmissionProcessor;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class LinkSubmittedConsumer {

    private final ObjectMapper objectMapper;

    private final LinkSubmissionProcessor processor;
    private final ExtractionRetryPublisher extractionRetryPublisher;

    @RabbitListener(queues = RabbitMQConfig.EXTRACTION_QUEUE)
    public void consume(Message message, Channel channel) throws IOException {

        long deliveryTag=message.getMessageProperties().getDeliveryTag();

        log.debug("Received link submission delivery tag {}", deliveryTag);

        LinkSubmittedEvent event;

        try{
            event=objectMapper.readValue(
                    message.getBody(),
                    LinkSubmittedEvent.class
            );
            log.info("Event deserialized successfully. Event ID: {}, Link ID: {}",
                    event.eventId(), event.linkId());
        }
        catch(Exception e){
            log.error(
                    "Failed to process link submitted event",
                    e
            );
           moveToDlqAndAck(
                   message,
                   channel,
                   deliveryTag,
                   "Malformed event JSON"
           );
           return;
        }
        try{
            log.info("Starting extraction for link: {}", event.linkId());

            processor.process(event);

            log.info("Extraction processing completed for link: {}",
                    event.linkId());

            channel.basicAck(deliveryTag,false);
        }
        catch (UnsafeUrlException | ExtractionException ex){
            log.error("Permanent extraction failure for link: {}",
                    event.linkId(), ex);

            moveToDlqAndAck(
                    message,
                    channel,
                    deliveryTag,
                    ex.getMessage(),
                    event,
                    "EXTRACTION_FAILED"
            );
        }
        catch (RetryableExtractionException exception){
            log.error("Retryable extraction failure for link: {}",
                    event.linkId(), exception);
            retryAndAck(message, event, channel, deliveryTag);
        }
        catch (Exception exception){
            log.error("Unexpected extraction failure for link: {}",
                    event.linkId(), exception);

            retryAndAck(message, event, channel, deliveryTag);
        }
    }

    private void retryAndAck(Message message,
                             LinkSubmittedEvent event,
                             Channel channel,
                             long deliveryTag) throws IOException {
        boolean exhausted = extractionRetryPublisher.retriesExhausted(message);
        try {
            extractionRetryPublisher.retry(message);
            if (exhausted) {
                extractionRetryPublisher.publishFailureEvent(event, "RETRIES_EXHAUSTED");
            }
            log.info("Message successfully published to retry/DLQ queue");
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Retry publishing failed; trying confirmed DLQ fallback", e);
            try {
                extractionRetryPublisher.publishToDlq(message, "Retry forwarding failed");
                extractionRetryPublisher.publishFailureEvent(event, exhausted ? "RETRIES_EXHAUSTED" : "RETRY_FORWARDING_FAILED");
                channel.basicAck(deliveryTag, false);
            } catch (Exception terminalFailure) {
                terminalFailure.addSuppressed(e);
                log.error("Retry, failure event, and DLQ forwarding failed; requeuing original message", terminalFailure);
                channel.basicNack(deliveryTag, false, true);
            }
        }
    }

    private void moveToDlqAndAck(Message message,
                                 Channel channel,
                                 long deliveryTag,
                                 String reason) throws IOException {
        moveToDlqAndAck(message, channel, deliveryTag, reason, null, null);
    }

    private void moveToDlqAndAck(Message message,
                                 Channel channel,
                                 long deliveryTag,
                                 String reason,
                                 LinkSubmittedEvent event,
                                 String failureCode) throws IOException {
        try {
            extractionRetryPublisher.publishToDlq(message, reason);
            if (event != null) {
                extractionRetryPublisher.publishFailureEvent(event, failureCode);
            }
            log.info("Message successfully published to DLQ and failure event. Reason: {}", reason);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("DLQ/failure event publishing failed. Requeuing original message", e);
            channel.basicNack(deliveryTag, false, true);
        }
    }
}
