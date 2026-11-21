package com.linklens.summarizationservice.service;

import com.linklens.summarizationservice.entity.Summary;
import com.linklens.summarizationservice.repo.SummaryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SummaryQueryServiceTest {

    @Mock
    SummaryRepository summaryRepository;

    @InjectMocks
    SummaryQueryService service;

    @Test
    void listUsesOwnerScopedPageAndMapsDto() {
        Summary summary = summary("alice");
        when(summaryRepository.findAllByUserId(eq("alice"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(summary)));

        var page = service.list("alice", 1, 10, "createdAt", Sort.Direction.DESC);

        assertEquals(1, page.totalElements());
        assertEquals(summary.getId(), page.content().getFirst().id());
        ArgumentCaptor<PageRequest> request = ArgumentCaptor.forClass(PageRequest.class);
        verify(summaryRepository).findAllByUserId(eq("alice"), request.capture());
        assertEquals(1, request.getValue().getPageNumber());
        assertEquals(Sort.Direction.DESC, request.getValue().getSort().getOrderFor("createdAt").getDirection());
    }

    @Test
    void getScopesBySummaryIdAndReturnsNotFoundForNonOwner() {
        UUID id = UUID.randomUUID();
        when(summaryRepository.findByIdAndUserId(id, "alice")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.get("alice", id));

        assertEquals(404, exception.getStatusCode().value());
        verify(summaryRepository).findByIdAndUserId(id, "alice");
        verify(summaryRepository, never()).findById(id);
    }

    @Test
    void getByLinkScopesByOwner() {
        Summary summary = summary("alice");
        when(summaryRepository.findByLinkIdAndUserId(summary.getLinkId(), "alice"))
                .thenReturn(Optional.of(summary));

        var response = service.getByLink("alice", summary.getLinkId());

        assertEquals(summary.getId(), response.id());
        assertEquals(summary.getLinkId(), response.linkId());
        verify(summaryRepository).findByLinkIdAndUserId(summary.getLinkId(), "alice");
    }

    @Test
    void rejectsInvalidPageSizeAndSortField() {
        assertThrows(ResponseStatusException.class,
                () -> service.list("alice", 0, 101, "createdAt", Sort.Direction.ASC));
        assertThrows(ResponseStatusException.class,
                () -> service.list("alice", 0, 10, "userId", Sort.Direction.ASC));
        verifyNoInteractions(summaryRepository);
    }

    private Summary summary(String userId) {
        return Summary.builder()
                .id(UUID.randomUUID())
                .linkId(UUID.randomUUID())
                .contentId(UUID.randomUUID())
                .userId(userId)
                .title("Page title")
                .summary("Short summary")
                .keyTakeaways(List.of("Point"))
                .readingTimeMinutes(2)
                .wordCount(100)
                .aiProvider("ollama")
                .aiModel("qwen2.5:3b")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
