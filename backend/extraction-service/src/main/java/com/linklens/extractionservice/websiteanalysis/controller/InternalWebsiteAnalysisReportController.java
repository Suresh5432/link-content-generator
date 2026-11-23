package com.linklens.extractionservice.websiteanalysis.controller;

import com.linklens.extractionservice.websiteanalysis.dto.WebsiteAnalysisReportResponse;
import com.linklens.extractionservice.websiteanalysis.service.WebsiteAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/website-analyses")
public class InternalWebsiteAnalysisReportController {

    private final WebsiteAnalysisService service;

    @GetMapping("/{id}/report")
    public WebsiteAnalysisReportResponse report(@PathVariable UUID id, @RequestParam String userId) {
        return service.report(userId, id);
    }

    @PatchMapping("/{id}/report")
    public void updateReport(
            @PathVariable UUID id,
            @RequestParam String userId,
            @RequestBody String report) {
        service.updateReport(userId, id, report);
    }
}
