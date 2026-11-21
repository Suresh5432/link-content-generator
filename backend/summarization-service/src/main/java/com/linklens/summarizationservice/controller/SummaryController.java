package com.linklens.summarizationservice.controller;

import com.linklens.summarizationservice.dto.SummaryResponse;
import com.linklens.summarizationservice.dto.PagedSummaryResponse;
import com.linklens.summarizationservice.service.SummaryQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/summaries")
public class SummaryController {

    private final SummaryQueryService summaryQueryService;

    @GetMapping
    public PagedSummaryResponse<SummaryResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        return summaryQueryService.list(jwt.getSubject(), page, size, sortBy, direction);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public SummaryResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return summaryQueryService.get(jwt.getSubject(), id);
    }

    @GetMapping("/link/{linkId}")
    public SummaryResponse getByLink(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID linkId) {
        return summaryQueryService.getByLink(jwt.getSubject(), linkId);
    }
}
