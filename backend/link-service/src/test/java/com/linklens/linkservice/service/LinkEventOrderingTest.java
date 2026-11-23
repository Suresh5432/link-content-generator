package com.linklens.linkservice.service;

import com.linkcontext.contracts.events.SummaryGeneratedEvent;
import com.linklens.linkservice.entity.Link;
import com.linklens.linkservice.entity.ProcessedEvent;
import com.linklens.linkservice.enums.LinkStatus;
import com.linklens.linkservice.repo.LinkRepository;
import com.linklens.linkservice.repo.ProcessedEventRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LinkEventOrderingTest {

    @Test
    void staleSummaryAfterFailureDoesNotCompleteLink() {
        LinkRepository links = mock(LinkRepository.class);
        ProcessedEventRepository processedEvents = mock(ProcessedEventRepository.class);
        UUID eventId = UUID.randomUUID();
        UUID linkId = UUID.randomUUID();
        Link link = link(linkId, LinkStatus.SUMMARIZATION_FAILED);
        when(processedEvents.existsById(eventId)).thenReturn(false);
        when(links.findById(linkId)).thenReturn(Optional.of(link));

        new SummaryGeneratedService(links, processedEvents).process(
                summaryEvent(eventId, linkId));

        assertEquals(LinkStatus.SUMMARIZATION_FAILED, link.getStatus());
        verify(processedEvents).save(any(ProcessedEvent.class));
        verify(links, never()).save(any(Link.class));
    }

    @Test
    void lateFailureAfterCompletionDoesNotRegressLink() {
        LinkRepository links = mock(LinkRepository.class);
        ProcessedEventRepository processedEvents = mock(ProcessedEventRepository.class);
        UUID eventId = UUID.randomUUID();
        UUID linkId = UUID.randomUUID();
        Link link = link(linkId, LinkStatus.COMPLETED);
        when(processedEvents.existsById(eventId)).thenReturn(false);
        when(links.findById(linkId)).thenReturn(Optional.of(link));

        new LinkFailureService(links, processedEvents).markSummarizationFailed(
                eventId, linkId, "user-1", "AI_TIMEOUT", false);

        assertEquals(LinkStatus.COMPLETED, link.getStatus());
        verify(processedEvents).save(any(ProcessedEvent.class));
        verify(links, never()).save(any(Link.class));
    }

    private static Link link(UUID id, LinkStatus status) {
        return Link.builder()
                .id(id)
                .userId("user-1")
                .originalUrl("https://example.com/article")
                .normalizedUrl("https://example.com/article")
                .status(status)
                .build();
    }

    private static SummaryGeneratedEvent summaryEvent(UUID eventId, UUID linkId) {
        return new SummaryGeneratedEvent(eventId, linkId, "user-1", "summary",
                List.of("takeaway"), 2, Instant.now(), 1);
    }
}
