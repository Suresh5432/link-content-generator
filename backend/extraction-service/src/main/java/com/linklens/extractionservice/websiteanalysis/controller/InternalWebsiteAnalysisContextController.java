package com.linklens.extractionservice.websiteanalysis.controller;

import com.linklens.extractionservice.websiteanalysis.dto.WebsiteAnalysisContextResponse;
import com.linklens.extractionservice.websiteanalysis.service.WebsiteAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/users")
public class InternalWebsiteAnalysisContextController {

    private final WebsiteAnalysisService service;

    @GetMapping("/{userId}/website-analysis-context")
    public WebsiteAnalysisContextResponse context(@PathVariable String userId,
                                                  @RequestParam(defaultValue = "5") int limit) {
        return service.assistantContext(userId, limit);
    }
}
