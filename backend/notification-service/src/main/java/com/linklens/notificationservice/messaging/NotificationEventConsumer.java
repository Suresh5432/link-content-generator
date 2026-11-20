package com.linklens.notificationservice.messaging;

import com.linkcontext.contracts.events.ExtractionFailedEvent;
import com.linkcontext.contracts.events.SummarizationFailedEvent;
import com.linkcontext.contracts.events.SummaryGeneratedEvent;
import com.linklens.notificationservice.config.RabbitMqConfig;
import com.linklens.notificationservice.service.NotificationProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {

    private final NotificationProcessor notificationProcessor;

    @RabbitListener(queues = RabbitMqConfig.SUMMARY_QUEUE)
    public void consumeSummaryGenerated(SummaryGeneratedEvent event){

        log.info("Received summary generated event: {}", event.eventId());

        notificationProcessor.process(event);
    }

    @RabbitListener(queues = RabbitMqConfig.EXTRACTION_FAILED_QUEUE)
    public void consumeExtractionFailed(ExtractionFailedEvent event) {

        log.info("Received extraction failed event: {}", event.eventId());

        notificationProcessor.process(event);
    }

    @RabbitListener(queues = RabbitMqConfig.SUMMARIZATION_FAILED_QUEUE)
    public void consumeSummarizationFailed(SummarizationFailedEvent event){

        log.info(
                "Received summarization failed event: {}",
                event.eventId());

        notificationProcessor.process(event);
    }
}
