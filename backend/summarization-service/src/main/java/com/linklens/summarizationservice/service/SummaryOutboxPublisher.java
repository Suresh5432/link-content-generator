package com.linklens.summarizationservice.service;

import com.linklens.summarizationservice.entity.SummaryOutboxEvent;
import com.linklens.summarizationservice.repo.SummaryOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class SummaryOutboxPublisher {

    private final SummaryOutboxRepository summaryOutboxRepository;

    private final RabbitTemplate rabbitTemplate;

    @Value("${summarization.outbox.confirm-timeout-seconds}")
    private long confirmTimeoutSeconds;

    @Transactional
    public int publishPendingEvents(){

        List<SummaryOutboxEvent> events=
                summaryOutboxRepository.findPendingEvents(
                        Instant.now(),
                        PageRequest.of(0,20));
        int published=0;

        for (SummaryOutboxEvent event : events) {
            try {

                publishedAndConfirm(event);

                event.markPublished();

                published++;

                log.info(
                        "Summary outbox event published. id={}, type={}",
                        event.getId(),
                        event.getEventType()
                        );
            }
            catch (Exception exception){

                if (exception instanceof InterruptedException){
                    Thread.currentThread().interrupt();
                }

                event.recordFailure(exception.getMessage());

                log.error(
                        "Failed to publish summary outbox event. id={}",
                        event.getId(),
                        exception
                );

                if (Thread.currentThread().isInterrupted()){
                    break;
                }
            }
        }
        return  published;
    }

    private void publishedAndConfirm(SummaryOutboxEvent event) throws Exception {

        CorrelationData correlationData=
                new CorrelationData(event.getId().toString());

        MessagePostProcessor processor=message -> {
            message.getMessageProperties()
                    .setContentType("application./json");

            message.getMessageProperties()
                    .setDeliveryMode(MessageDeliveryMode.PERSISTENT);

            message.getMessageProperties()
                    .setMessageId(event.getId().toString());

            message.getMessageProperties()
                    .setType(event.getEventType());

            return message;
        };

        rabbitTemplate.convertAndSend(
                "link.events",
                event.getEventType(),
                event.getPayload(),
                processor,
                correlationData
        );

        CorrelationData.Confirm confirm=
                correlationData.getFuture()
                        .get(confirmTimeoutSeconds, TimeUnit.SECONDS);

        if (!confirm.ack()){
            throw new IllegalStateException(
                    "RabbitMQ rejected message: "+confirm.reason()
            );
        }

        if (correlationData.getReturned()!=null){
            throw new IllegalStateException(
                    "RabbitMQ could not route the message"
            );
        }
    }
}
