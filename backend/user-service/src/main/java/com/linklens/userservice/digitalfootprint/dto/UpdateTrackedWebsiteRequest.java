package com.linklens.userservice.digitalfootprint.dto;

public record UpdateTrackedWebsiteRequest(
        String name,
        String domain,
        String url,
        String category,
        String accountStatus,
        String privacyPolicyUrl,
        String deletionPolicyUrl,
        String riskLevel,
        String dataCategories,
        String trackingTechnologies,
        String deletionRequestStatus,
        String fromAnalysisId
) {}
