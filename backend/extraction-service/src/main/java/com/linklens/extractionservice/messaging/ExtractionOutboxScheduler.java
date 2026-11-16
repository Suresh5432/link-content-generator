package com.linklens.extractionservice.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExtractionOutboxScheduler {

    private final ExtractionOutboxPublisher publisher;

    @Scheduled(fixedDelayString = "${extraction.outbox.fixed-delay-ms:1000}")
    public void publishPendingEvents(){
        publisher.publishBatch();
    }

}
