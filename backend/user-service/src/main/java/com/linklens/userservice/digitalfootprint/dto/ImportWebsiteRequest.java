package com.linklens.userservice.digitalfootprint.dto;

import jakarta.validation.constraints.NotBlank;

public record ImportWebsiteRequest(
        @NotBlank String analysisId,
        @NotBlank String name,
        @NotBlank String domain,
        @NotBlank String url,
        String category,
        String riskLevel,
        String dataCategories,
        String trackingTechnologies
) {}
