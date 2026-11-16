package com.linklens.linkservice.messaging;

import com.linklens.linkservice.service.OutboxPublisherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxScheduler {

    private final OutboxPublisherService publisherService;

    @Scheduled(fixedDelayString = "${outbox.publisher.fixed-delay:1000}")
    public void publishPendingEvents(){
        try{
            int published = publisherService.publishBatch();

            if (published>0){
                log.info("Published {} outbox events", published);
            }
        }
        catch (Exception e){
            log.error("Outbox publishing cycle failed", e);
        }
    }
}
