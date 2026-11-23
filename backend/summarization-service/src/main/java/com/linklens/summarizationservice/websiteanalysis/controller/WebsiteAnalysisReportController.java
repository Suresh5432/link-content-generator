package com.linklens.summarizationservice.websiteanalysis.controller;

import com.linklens.summarizationservice.websiteanalysis.WebsiteAnalysisReportService;
import com.linklens.summarizationservice.websiteanalysis.dto.WebsiteAnalysisReportRequest;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/website-analysis-reports")
public class WebsiteAnalysisReportController {

    private final WebsiteAnalysisReportService service;

    @PostMapping
    public String generate(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody WebsiteAnalysisReportRequest request) {
        return service.generateAndPersist(request.analysisId(), jwt.getSubject());
    }
}
