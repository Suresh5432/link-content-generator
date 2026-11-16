package com.linklens.extractionservice.messaging;

import com.linkcontext.contracts.events.LinkSubmittedEvent;
import com.linklens.extractionservice.config.RabbitMQConfig;
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

    @RabbitListener(queues = RabbitMQConfig.EXTRACTION_QUEUE)
    public void consume(Message message, Channel channel) throws IOException {
        long deliveryTag=message.getMessageProperties().getDeliveryTag();

        try{
            LinkSubmittedEvent event=objectMapper.readValue(
                    message.getBody(),
                    LinkSubmittedEvent.class
            );

            processor.process(event);

            channel.basicAck(deliveryTag,false);
        }
        catch(Exception e){
            channel.basicNack(deliveryTag, false, true);
        }
    }
}
