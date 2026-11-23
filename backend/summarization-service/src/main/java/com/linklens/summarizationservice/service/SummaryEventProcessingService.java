package com.linklens.summarizationservice.service;

import com.linkcontext.contracts.events.ContentExtractedEvent;
import com.linkcontext.contracts.events.SummarizationFailedEvent;
import com.linklens.summarizationservice.entity.SummaryOutboxEvent;
import com.linklens.summarizationservice.repo.ProcessedEventRepository;
import com.linklens.summarizationservice.repo.SummaryOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SummaryEventProcessingService {

    private final ProcessedEventRepository processedEventRepository;

    private final SummaryService summaryService;

    private final SummaryOutboxRepository outboxRepository;

    private final ObjectMapper objectMapper;

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
                null,
                event.canonicalUrl()
        );
        log.info("Summary generated for event {} and link {}", event.eventId(), event.linkId());

    }

    @Transactional
    public void recordTerminalFailure(ContentExtractedEvent event) {
        UUID failureEventId = UUID.nameUUIDFromBytes(
                ("summary-failed:" + event.eventId()).getBytes(StandardCharsets.UTF_8));
        if (outboxRepository.existsById(failureEventId)) {
            return;
        }
        SummarizationFailedEvent failed = new SummarizationFailedEvent(
                failureEventId,
                event.linkId(),
                event.userId(),
                "SUMMARY_PROCESSING_FAILED",
                false,
                Instant.now(),
                1);
        try {
            outboxRepository.save(new SummaryOutboxEvent(
                    failureEventId,
                    event.linkId(),
                    "summarization.failed.v1",
                    objectMapper.writeValueAsString(failed)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to persist terminal summary failure event", exception);
        }
    }

}
