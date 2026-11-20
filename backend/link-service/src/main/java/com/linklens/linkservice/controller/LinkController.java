package com.linklens.linkservice.controller;

import com.linklens.linkservice.dto.CreateLinkRequest;
import com.linklens.linkservice.dto.LinkResponse;
import com.linklens.linkservice.dto.PagedResponse;
import com.linklens.linkservice.service.LinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import com.linklens.linkservice.enums.LinkStatus;

import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/links")
public class LinkController {

    private final LinkService linkService;

    @PostMapping
    public ResponseEntity<LinkResponse> createLink(
            @Valid @RequestBody CreateLinkRequest createLinkRequest,
            @RequestHeader(
                    value= "Idempotency-Key",
                    required = false) String idempotencyKey,
            @AuthenticationPrincipal Jwt jwt) {

        LinkResponse linkResponse = linkService.createLink(
                    createLinkRequest,
                jwt.getSubject(),
                idempotencyKey
        );
        log.debug("Created link request completed for user {} with link {}", jwt.getSubject(), linkResponse.getId());

        URI location = URI.create("/api/v1/links/" + linkResponse.getId());

        return ResponseEntity.created(location).body(linkResponse);
    }

    @GetMapping
    public ResponseEntity<PagedResponse<LinkResponse>> getLinks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) LinkStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction,
            @AuthenticationPrincipal Jwt jwt){
        PagedResponse<LinkResponse> response = linkService.searchLinks(jwt.getSubject(), page, size, status, q, sortBy, direction);
        log.debug("Listed links for user {} (page {}, size {})", jwt.getSubject(), page, size);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<LinkResponse> retryLink(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(linkService.retryLink(id, jwt.getSubject()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LinkResponse> getLink(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt){

        LinkResponse response = linkService.getLink(id, jwt.getSubject());
        log.debug("Retrieved link {} for user {}", id, jwt.getSubject());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLink(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt){
        linkService.deleteLink(id, jwt.getSubject());
        log.debug("Deleted link {} for user {}", id, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }
}
