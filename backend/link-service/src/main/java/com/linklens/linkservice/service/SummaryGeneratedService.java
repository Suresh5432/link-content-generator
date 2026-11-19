package com.linklens.linkservice.service;

import com.linkcontext.contracts.events.SummaryGeneratedEvent;
import com.linklens.linkservice.entity.Link;
import com.linklens.linkservice.entity.ProcessedEvent;
import com.linklens.linkservice.enums.LinkStatus;
import com.linklens.linkservice.repo.LinkRepository;
import com.linklens.linkservice.repo.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Slf4j
@RequiredArgsConstructor
public class SummaryGeneratedService {

    private final LinkRepository linkRepository;

    private final ProcessedEventRepository processedEventRepository;

    @Transactional
    public void process(SummaryGeneratedEvent event){

        if (processedEventRepository.existsById(event.eventId())){
            log.info("Duplicate event received: {}", event.eventId());
            return;
        }

        Link link=linkRepository.findById(event.linkId())
                .orElseThrow(()->
                        new IllegalStateException(
                                "Link not found: " +event.linkId()
                        )
                );

        if (!link.getUserId().equals(event.userId())){
            throw new IllegalStateException(
                    "User ID mismatch for link: "+event.linkId()
            );
        }

        if (link.getStatus()== LinkStatus.COMPLETED){

            log.info("Link {} is already completed, Ignoring duplicate event {}",
                    event.linkId(),event.eventId());

            recordProcessedEvent(event);

            return;
        }

        link.updateStatus(LinkStatus.COMPLETED);

        recordProcessedEvent(event);

        linkRepository.save(link);


        log.info("Link {} successfully completed. Summary event: {}",event.linkId(),event.eventId());
    }

    private void recordProcessedEvent(SummaryGeneratedEvent event){

        ProcessedEvent processedEvent = ProcessedEvent.builder()
                .eventId(event.eventId())
                .eventType("summary.generated.v1")
                .build();

        processedEventRepository.save(processedEvent);
    }
}
