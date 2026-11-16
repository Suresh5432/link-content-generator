package com.linklens.extractionservice.service;

import com.linkcontext.contracts.events.ContentExtractedEvent;
import com.linklens.extractionservice.dto.ExtractedPage;
import com.linklens.extractionservice.entity.ExtractionContent;
import com.linklens.extractionservice.entity.ExtractionOutboxEvent;
import com.linklens.extractionservice.entity.ProcessedEvent;
import com.linklens.extractionservice.repo.ExtractionContentRepository;
import com.linklens.extractionservice.repo.ExtractionOutboxRepository;
import com.linklens.extractionservice.repo.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExtractionPersistenceService {

    private final ExtractionContentRepository extractionContentRepository;

    private final ExtractionOutboxRepository extractionOutboxRepository;

    private final ProcessedEventRepository processedEventRepository;

    private final ObjectMapper objectMapper;

    @Transactional
    public UUID saveExtraction(
            UUID eventId,
            UUID linkId,
            String userId,
            String sourceUrl,
            ExtractedPage page){

        if (processedEventRepository.existsByEventId(eventId)){
            return extractionContentRepository.findByLinkId(linkId)
                    .orElseThrow()
                    .getId();
        }

        if (extractionContentRepository.existsByLinkId(linkId)){
            throw new IllegalStateException("Link already has extracted content but event is not recorded");
        }

        UUID contentId=UUID.randomUUID();

        ExtractionContent  content=ExtractionContent.builder()
                .id(contentId)
                .linkId(linkId)
                .userId(userId)
                .sourceUrl(sourceUrl)
                .canonicalUrl(page.getCanonicalUrl())
                .title(page.getTitle())
                .description(page.getDescription())
                .contentType(page.getContentType())
                .language(page.getLanguage())
                .extractedText(page.getExtractedText())
                .wordCount(page.getWordCount())
                .build();

        extractionContentRepository.save(content);

        ContentExtractedEvent event =new ContentExtractedEvent(
                UUID.randomUUID(),
                linkId,
                contentId,
                userId,
                page.getTitle(),
                page.getCanonicalUrl(),
                page.getContentType(),
                page.getWordCount(),
                Instant.now(),
                1
        );

        try{
            String payload=objectMapper.writeValueAsString(event);

            ExtractionOutboxEvent outboxEvent=
                    ExtractionOutboxEvent.builder()
                            .id(UUID.randomUUID())
                            .aggregateId(linkId)
                            .eventType("content.extracted.v1")
                            .payload(payload)
                            .build();
            extractionOutboxRepository.save(outboxEvent);

            processedEventRepository.save(ProcessedEvent.builder()
                            .eventId(eventId)
                            .eventType("content.extracted.v1")
                            .build());
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to serialize extraction event", e);
        }

        return contentId;
    }

}
