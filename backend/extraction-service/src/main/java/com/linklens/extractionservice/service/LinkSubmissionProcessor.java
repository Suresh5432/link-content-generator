package com.linklens.extractionservice.service;

import com.linkcontext.contracts.events.LinkSubmittedEvent;
import com.linklens.extractionservice.dto.ExtractionResult;
import com.linklens.extractionservice.entity.ProcessedEvent;
import com.linklens.extractionservice.repo.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LinkSubmissionProcessor {

    private final ProcessedEventRepository processedEventRepository;

    private final ExtractionService extractionService;

    private final ExtractionPersistenceService extractionPersistenceService;

    public void process(LinkSubmittedEvent event){

        log.info("Processing link event: {}", event.eventId());

        if (processedEventRepository.existsByEventId(event.eventId())){

            log.info("Event already processed: {}", event.eventId());

            return;
        }

        log.info("Starting webpage extraction: {}", event.url());

        ExtractionResult result=extractionService.extract(event.url());

        log.info("Webpage extraction completed for link: {}",
                event.linkId());

       extractionPersistenceService.saveExtraction(
               event.eventId(),
               event.linkId(),
               event.userId(),
               event.url(),
               result.getPage()
       );

        log.info("Extraction persisted successfully for link: {}",
                event.linkId());

    }
}
