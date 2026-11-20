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
                    ex.getMessage()
            );
        }
        catch (RetryableExtractionException exception){
            log.error("Retryable extraction failure for link: {}",
                    event.linkId(), exception);
            retryAndAck(message, channel,deliveryTag);
        }
        catch (Exception exception){
            log.error("Unexpected extraction failure for link: {}",
                    event.linkId(), exception);

            retryAndAck(message, channel, deliveryTag);
        }
    }

    private void retryAndAck(Message message,
                             Channel channel,
                             long deliveryTag) throws IOException {
        try{
            extractionRetryPublisher.retry(message);

            log.info("Message successfully published to retry queue");

            channel.basicAck(deliveryTag,false);
        } catch (Exception e) {
            log.error("Retry publishing failed. Requeuing original message",
                    e);

            channel.basicNack(deliveryTag,false,true);
        }
    }

    private void moveToDlqAndAck(Message message,
                                 Channel channel,
                                 long deliveryTag,
                                 String reason) throws IOException {
        try{
            extractionRetryPublisher.publishToDlq(message, reason);

            log.info("Message successfully published to DLQ. Reason: {}",
                    reason);

            channel.basicAck(deliveryTag,false);

        } catch (Exception e) {

            log.error("DLQ publishing failed. Requeuing original message",
                    e);

            channel.basicNack(deliveryTag,false,true);
        }
    }
}
