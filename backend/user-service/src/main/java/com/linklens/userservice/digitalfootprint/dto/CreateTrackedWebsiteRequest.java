package com.linklens.userservice.digitalfootprint.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTrackedWebsiteRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 255) String domain,
        @NotBlank @Size(max = 2048) String url,
        @Size(max = 100) String category,
        @Size(max = 40) String accountStatus,
        @Size(max = 2048) String privacyPolicyUrl,
        @Size(max = 2048) String deletionPolicyUrl,
        @Size(max = 20) String riskLevel,
        String fromAnalysisId,
        String dataCategories,
        String trackingTechnologies
) {}
