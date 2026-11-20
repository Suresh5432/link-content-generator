package com.linklens.linkservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linklens.linkservice.dto.CreateLinkRequest;
import com.linkcontext.contracts.EventRoutingKey;
import com.linklens.linkservice.entity.Link;
import com.linklens.linkservice.enums.LinkStatus;
import com.linklens.linkservice.exception.IdempotencyConflictException;
import com.linklens.linkservice.exception.ResourceNotFoundException;
import com.linklens.linkservice.repo.LinkRepository;
import com.linklens.linkservice.repo.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;
import com.linklens.linkservice.entity.OutboxEvent;

class LinkServiceTest {
    private LinkRepository links;
    private OutboxRepository outbox;
    private LinkService service;

    @BeforeEach
    void setUp() {
        links = mock(LinkRepository.class);
        outbox = mock(OutboxRepository.class);
        service = new LinkService(links, outbox, new UrlNormalizer(), new ObjectMapper().findAndRegisterModules());
    }

    @Test
    void idempotentCreateReturnsExistingForSameOwnerAndUrl() {
        CreateLinkRequest request = request("https://example.com/a");
        Link existing = link("user-1", "https://example.com/a", LinkStatus.COMPLETED);
        when(links.findByUserIdAndIdempotencyKeyAndDeletedAtIsNull("user-1", "key-1")).thenReturn(Optional.of(existing));

        var response = service.createLink(request, "user-1", "key-1");

        assertEquals(existing.getId(), response.getId());
        verify(links, never()).save(any());
        verifyNoInteractions(outbox);
    }

    @Test
    void idempotencyKeyCannotBeReusedForDifferentUrl() {
        Link existing = link("user-1", "https://example.com/a", LinkStatus.COMPLETED);
        when(links.findByUserIdAndIdempotencyKeyAndDeletedAtIsNull("user-1", "key-1")).thenReturn(Optional.of(existing));
        assertThrows(IdempotencyConflictException.class, () -> service.createLink(request("https://example.com/b"), "user-1", "key-1"));
    }

    @Test
    void rejectsInvalidIdempotencyKey() {
        assertThrows(IllegalArgumentException.class, () -> service.createLink(request("https://example.com"), "u", "has spaces"));
        verifyNoInteractions(links);
    }

    @Test
    void listingValidatesPaginationAndSort() {
        assertThrows(IllegalArgumentException.class, () -> service.searchLinks("u", -1, 10, null, null, "createdAt", Sort.Direction.DESC));
        assertThrows(IllegalArgumentException.class, () -> service.searchLinks("u", 0, 101, null, null, "createdAt", Sort.Direction.DESC));
        assertThrows(IllegalArgumentException.class, () -> service.searchLinks("u", 0, 10, null, null, "originalUrl", Sort.Direction.DESC));
        verifyNoInteractions(links);
    }

    @Test
    void searchIsPagedAndMapsOwnedResults() {
        Link result = link("u", "https://example.com/a", LinkStatus.COMPLETED);
        when(links.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(result), PageRequest.of(0, 10), 1));
        var response = service.searchLinks("u", 0, 10, LinkStatus.COMPLETED, "example", "createdAt", Sort.Direction.DESC);
        assertEquals(1, response.content().size());
        assertEquals(result.getId(), response.content().getFirst().getId());
        verify(links).findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class));
    }

    @Test
    void retryOnlyAllowsFailedLinkAndEnqueuesSubmission() {
        UUID id = UUID.randomUUID();
        Link failed = link("u", "https://example.com/a", LinkStatus.EXTRACTION_FAILED);
        failed.setId(id);
        when(links.findByIdAndUserIdAndDeletedAtIsNull(id, "u")).thenReturn(Optional.of(failed));
        var response = service.retryLink(id, "u");
        assertEquals(LinkStatus.PENDING_EXTRACTION, response.getStatus());
        ArgumentCaptor<OutboxEvent> captured = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outbox).save(captured.capture());
        assertEquals(id, captured.getValue().getAggregateId());
        assertEquals(EventRoutingKey.LINK_SUBMITTED, captured.getValue().getEventType());
        assertTrue(captured.getValue().getPayload().contains("example.com/a"));
    }

    @Test
    void retryHidesForeignLinksAndRejectsNonFailedState() {
        UUID id = UUID.randomUUID();
        when(links.findByIdAndUserIdAndDeletedAtIsNull(id, "other")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.retryLink(id, "other"));
        when(links.findByIdAndUserIdAndDeletedAtIsNull(id, "u")).thenReturn(Optional.of(link("u", "https://example.com", LinkStatus.COMPLETED)));
        assertThrows(IllegalArgumentException.class, () -> service.retryLink(id, "u"));
        verifyNoInteractions(outbox);
    }

    private static CreateLinkRequest request(String url) { CreateLinkRequest r = new CreateLinkRequest(); r.setUrl(url); return r; }
    private static Link link(String owner, String url, LinkStatus status) {
        return Link.builder().id(UUID.randomUUID()).userId(owner).originalUrl(url).normalizedUrl(url).status(status).build();
    }
}
