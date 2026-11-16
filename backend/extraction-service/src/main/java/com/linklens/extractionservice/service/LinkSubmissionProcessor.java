package com.linklens.extractionservice.service;

import com.linkcontext.contracts.events.LinkSubmittedEvent;
import com.linklens.extractionservice.dto.ExtractionResult;
import com.linklens.extractionservice.entity.ProcessedEvent;
import com.linklens.extractionservice.repo.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LinkSubmissionProcessor {

    private final ProcessedEventRepository processedEventRepository;

    private final ExtractionService extractionService;

    private final ExtractionPersistenceService extractionPersistenceService;

    @Transactional
    public void process(LinkSubmittedEvent event){

        if (processedEventRepository.existsByEventId(event.eventId())){
            return;
        }

        ExtractionResult result=extractionService.extract(event.url());

        extractionPersistenceService.saveExtraction(
                event.linkId(),
                event.userId(),
                event.url(),
                new ExtractionPersistenceService.ExtractionResultHolder(result.getPage())
        );

        processedEventRepository.save(ProcessedEvent.builder()
                .eventId(event.eventId())
                .eventType("link.submitted.v1")
                .build());
    }
}
