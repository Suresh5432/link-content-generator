package com.linklens.summarizationservice.service;

import com.linkcontext.contracts.events.ContentExtractedEvent;
import com.linklens.summarizationservice.repo.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SummaryEventProcessingService {

    private final ProcessedEventRepository processedEventRepository;

    private final SummaryService summaryService;

    public void process(ContentExtractedEvent event) {

        if (processedEventRepository.existsByEventId(event.eventId())) {
            log.info("Skipping already processed content event {}", event.eventId());
            return;
        }

        summaryService.generateSummary(
                event.eventId(),
                event.linkId(),
                event.contentId(),
                event.userId(),
                event.title(),
                null
        );
        log.info("Summary generated for event {} and link {}", event.eventId(), event.linkId());

    }

}
