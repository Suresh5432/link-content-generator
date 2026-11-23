package com.linklens.extractionservice.websiteanalysis.dto;

import com.linklens.extractionservice.websiteanalysis.entity.WebsiteFinding;
import com.linklens.extractionservice.websiteanalysis.entity.WebsitePage;

import java.util.List;
import java.util.UUID;

public record WebsiteAnalysisReportResponse(
        UUID analysisId,
        WebsiteAnalysisResponse analysis,
        List<WebsitePage> pages,
        List<WebsiteFinding> findings
) {}
