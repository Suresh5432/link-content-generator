package com.linklens.linkservice.service;

import com.linklens.linkservice.config.RabbitMQConfig;
import com.linklens.linkservice.entity.OutboxEvent;
import com.linklens.linkservice.repo.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class OutboxPublisherService {

    private final OutboxRepository outboxRepository;

    private final RabbitTemplate rabbitTemplate;

    private final Duration confirmTimeout;

    public OutboxPublisherService(OutboxRepository outboxRepository,
                                  RabbitTemplate rabbitTemplate,
                                  @Value("${outbox.publisher.confirm-timeout:5s}")
                                  Duration confirmTimeout) {
        this.outboxRepository = outboxRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.confirmTimeout = confirmTimeout;
    }

    @Transactional
    public int publishBatch(){

        List<OutboxEvent> events = outboxRepository.findUnpublishedBatchForUpdate(20);

        int publishedCount=0;

        for (OutboxEvent event : events) {
            try{
                publishAndConfirm(event);

                event.markPublished();

                publishedCount++;

                log.info("Published outbox event. eventId={}, eventType={}, aggregateId={}",
                        event.getId(),
                        event.getEventType(),
                        event.getAggregateId()
                );
            }
            catch (Exception ex){
                if (ex instanceof InterruptedException){
                    Thread.currentThread().interrupt();
                }
                event.recordFailure(ex.getMessage()!=null
                                    ? ex.getMessage()
                                    : ex.getClass().getSimpleName());

                log.error(
                        "Failed to publish outbox event. eventId={}, eventType={}",
                        event.getId(),
                        event.getEventType(),
                        ex
                );

                if (Thread.currentThread().isInterrupted()){
                    break;
                }
            }
        }

        return publishedCount;
    }

    private void publishAndConfirm(OutboxEvent event) throws Exception {

        CorrelationData correlationData=new CorrelationData(event.getId().toString());

        MessagePostProcessor messagePostProcessor=message->{

            message.getMessageProperties()
                    .setContentType("application/json");
            message.getMessageProperties()
                    .setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            message.getMessageProperties()
                    .setMessageId(event.getId().toString());
            message.getMessageProperties()
                    .setType(event.getEventType());

            return message;
        };

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.LINK_EVENTS_EXCHANGE,
                event.getEventType(),
                event.getPayload(),
                messagePostProcessor,
                correlationData
        );

        CorrelationData.Confirm confirm=correlationData
                .getFuture()
                .get(confirmTimeout.toMillis(), TimeUnit.MILLISECONDS);

        if (!confirm.ack()){
            throw new IllegalStateException("RabbitMQ rejected the message : "+ confirm.reason());
        }

        if (correlationData.getReturned()!=null){
            throw  new IllegalStateException("RabbitMQ could not route the message to a queue");
        }
    }
}
