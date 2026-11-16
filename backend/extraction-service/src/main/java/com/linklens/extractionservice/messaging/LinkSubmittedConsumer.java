package com.linklens.extractionservice.messaging;

import com.linkcontext.contracts.events.LinkSubmittedEvent;
import com.linklens.extractionservice.config.RabbitMQConfig;
import com.linklens.extractionservice.exception.ExtractionException;
import com.linklens.extractionservice.exception.RetryableExtractionException;
import com.linklens.extractionservice.exception.UnsafeUrlException;
import com.linklens.extractionservice.service.LinkSubmissionProcessor;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class LinkSubmittedConsumer {

    private final ObjectMapper objectMapper;

    private final LinkSubmissionProcessor processor;
    private final ExtractionRetryPublisher extractionRetryPublisher;

    @RabbitListener(queues = RabbitMQConfig.EXTRACTION_QUEUE)
    public void consume(Message message, Channel channel) throws IOException {

        long deliveryTag=message.getMessageProperties().getDeliveryTag();

        LinkSubmittedEvent event;

        try{
            event=objectMapper.readValue(
                    message.getBody(),
                    LinkSubmittedEvent.class
            );
        }
        catch(Exception e){
           moveToDlqAndAck(
                   message,
                   channel,
                   deliveryTag,
                   "Malformed event JSON"
           );
           return;
        }
        try{
            processor.process(event);
            channel.basicAck(deliveryTag,false);
        }
        catch (UnsafeUrlException | ExtractionException ex){
            moveToDlqAndAck(
                    message,
                    channel,
                    deliveryTag,
                    ex.getMessage()
            );
        }
        catch (RetryableExtractionException exception){
            retryAndAck(message, channel,deliveryTag);
        }
        catch (Exception exception){
            retryAndAck(message, channel, deliveryTag);
        }
    }

    private void retryAndAck(Message message,
                             Channel channel,
                             long deliveryTag) throws IOException {
        try{
            extractionRetryPublisher.retry(message);
            channel.basicAck(deliveryTag,false);
        } catch (Exception e) {
            channel.basicNack(deliveryTag,false,true);
        }
    }

    private void moveToDlqAndAck(Message message,
                                 Channel channel,
                                 long deliveryTag,
                                 String reason) throws IOException {
        try{
            extractionRetryPublisher.publishToDlq(message, reason);
            channel.basicAck(deliveryTag,false);
        } catch (Exception e) {
            channel.basicNack(deliveryTag,false,true);
        }
    }
}
