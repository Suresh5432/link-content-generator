package com.linklens.summarizationservice.service;

import com.linkcontext.contracts.events.ContentExtractedEvent;
import com.linklens.summarizationservice.entity.SummaryOutboxEvent;
import com.linklens.summarizationservice.repo.ProcessedEventRepository;
import com.linklens.summarizationservice.repo.SummaryOutboxRepository;
import com.linklens.summarizationservice.service.ai.AiInputValidator;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SummaryTerminalFailureTest {
    @Test
    void persistsIdempotentNonRetryableFailureToOutbox() throws Exception {
        ProcessedEventRepository processed = mock(ProcessedEventRepository.class);
        SummaryService summary = mock(SummaryService.class);
        SummaryOutboxRepository outbox = mock(SummaryOutboxRepository.class);
        ObjectMapper mapper = mock(ObjectMapper.class);
        UUID sourceId = UUID.randomUUID();
        ContentExtractedEvent event = new ContentExtractedEvent(sourceId, UUID.randomUUID(), UUID.randomUUID(),
                "user-1", "title", "https://example.org", "text/html", 20, Instant.now(), 1);
        when(outbox.existsById(any())).thenReturn(false, true);
        when(mapper.writeValueAsString(any())).thenReturn("{}");

        new SummaryEventProcessingService(processed, summary, outbox, mapper).recordTerminalFailure(event);
        new SummaryEventProcessingService(processed, summary, outbox, mapper).recordTerminalFailure(event);

        verify(outbox).save(any(SummaryOutboxEvent.class));
        verify(summary, never()).generateSummary(any(), any(), any(), any(), any(), any());
    }
}
