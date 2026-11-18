package com.linklens.summarizationservice.messaging;

import com.linklens.summarizationservice.service.SummaryOutboxPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SummaryOutboxScheduler {

    private final SummaryOutboxPublisher publisher;

    @Scheduled(
            fixedDelayString = "${summarization.outbox.fixed-delay-ms}"
    )
    public void publishPendingEvents(){
        try{
            int count= publisher.publishPendingEvents();

            if (count>0){
                log.info("Published {} summary outbox events", count);
            }
        }
        catch (Exception e){
            log.error(
                    "Summary outbox publishing cycle failed",e
            );
        }
    }
}
