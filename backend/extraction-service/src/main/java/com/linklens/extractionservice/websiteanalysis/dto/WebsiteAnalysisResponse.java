package com.linklens.extractionservice.websiteanalysis.dto;

import com.linklens.extractionservice.websiteanalysis.WebsiteAnalysisStatus;
import java.time.Instant;
import java.util.UUID;

public record WebsiteAnalysisResponse(
        UUID id,
        String url,
        String domain,
        String title,
        WebsiteAnalysisStatus status,
        String riskLevel,
        Integer confidence,
        String category,
        String report,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt
) {}
