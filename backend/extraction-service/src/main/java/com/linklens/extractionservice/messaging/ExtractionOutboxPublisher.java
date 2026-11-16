package com.linklens.extractionservice.messaging;

import com.linklens.extractionservice.config.RabbitMQConfig;
import com.linklens.extractionservice.entity.ExtractionOutboxEvent;
import com.linklens.extractionservice.repo.ExtractionOutboxRepository;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ExtractionOutboxPublisher {

    private final ExtractionOutboxRepository repository;

    private final RabbitTemplate rabbitTemplate;

    private final long confirmTimeoutSeconds;

    public ExtractionOutboxPublisher(
            ExtractionOutboxRepository repository,
            RabbitTemplate rabbitTemplate,
            @Value("${extraction.outbox.confirm-timeout-seconds:5}")
            long confirmTimeoutSeconds){
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
        this.confirmTimeoutSeconds = confirmTimeoutSeconds;
    }

    @Transactional
    public void publishBatch(){

        List<ExtractionOutboxEvent> events = repository.findPendingBatch(20);

        for (ExtractionOutboxEvent event : events) {
            publishOne(event);
        }
    }

    private void publishOne(ExtractionOutboxEvent event) {

        CorrelationData correlationData=
                new CorrelationData(event.getId().toString());

        try{
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXTRACTION_EXCHANGE,
                    event.getEventType(),
                    event.getPayload(),
                    message -> {
                        message.getMessageProperties()
                                .setMessageId(event.getId().toString());
                        message.getMessageProperties()
                                .setContentType("application/json");
                        message.getMessageProperties()
                                .setDeliveryMode(MessageDeliveryMode.PERSISTENT);

                        return message;
                    },correlationData
            );

            CorrelationData.Confirm confirm=correlationData
                    .getFuture()
                    .get(confirmTimeoutSeconds, TimeUnit.SECONDS);

            if(!confirm.ack()){
                throw new IllegalStateException("RabbitMQ did not confirm the event : "+confirm.reason());
            }

            if (correlationData.getReturned()!=null){
                throw  new IllegalStateException("RabbitMQ returned the event because it was unroutable ");
            }
            event.markPublished();
        }
        catch(Exception e){
            String error= e.getMessage()==null
                    ? e.getClass().getSimpleName()
                    : e.getMessage();
            event.recordFailure(error);
        }
    }
}
