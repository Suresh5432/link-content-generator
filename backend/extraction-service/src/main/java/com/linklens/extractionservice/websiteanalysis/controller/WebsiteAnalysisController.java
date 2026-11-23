package com.linklens.extractionservice.websiteanalysis.controller;

import com.linklens.extractionservice.websiteanalysis.dto.CreateWebsiteAnalysisRequest;
import com.linklens.extractionservice.websiteanalysis.dto.WebsiteAnalysisResponse;
import com.linklens.extractionservice.websiteanalysis.dto.WebsiteAnalysisReportResponse;
import com.linklens.extractionservice.websiteanalysis.entity.WebsiteFinding;
import com.linklens.extractionservice.websiteanalysis.entity.WebsitePage;
import com.linklens.extractionservice.websiteanalysis.service.WebsiteAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/website-analyses")
public class WebsiteAnalysisController {

    private final WebsiteAnalysisService service;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public WebsiteAnalysisResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateWebsiteAnalysisRequest request) {
        return service.create(jwt.getSubject(), request);
    }

    @GetMapping
    public Page<WebsiteAnalysisResponse> list(@AuthenticationPrincipal Jwt jwt,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return service.list(jwt.getSubject(), page, size);
    }

    @GetMapping("/{id}")
    public WebsiteAnalysisResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.get(jwt.getSubject(), id);
    }

    @GetMapping("/{id}/pages")
    public List<WebsitePage> pages(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.pages(jwt.getSubject(), id);
    }

    @GetMapping("/{id}/findings")
    public List<WebsiteFinding> findings(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.findings(jwt.getSubject(), id);
    }

    @GetMapping("/{id}/report")
    public WebsiteAnalysisReportResponse report(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.report(jwt.getSubject(), id);
    }

    @PostMapping("/{id}/retry")
    public WebsiteAnalysisResponse retry(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.retry(jwt.getSubject(), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        service.delete(jwt.getSubject(), id);
    }

    @PatchMapping("/{id}/report")
    public WebsiteAnalysisResponse updateReport(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @RequestBody String report) {
        return service.updateReport(jwt.getSubject(), id, report);
    }
}