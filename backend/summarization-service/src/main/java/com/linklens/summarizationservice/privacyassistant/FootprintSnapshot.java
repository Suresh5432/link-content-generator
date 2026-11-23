package com.linklens.summarizationservice.privacyassistant;

import java.util.List;

public record FootprintSnapshot(
        List<Website> websites,
        List<DataRecord> dataRecords,
        List<RequestRecord> requests,
        List<String> cookieDomains,
        long trackingCookieCount) {

    public record Website(
            String name,
            String domain,
            String category,
            String riskLevel,
            String accountStatus,
            String dataCategories,
            String trackingTechnologies,
            String deletionRequestStatus,
            String privacyPolicyUrl,
            String deletionPolicyUrl) {}

    public record DataRecord(
            String website,
            String dataCategory,
            String dataType,
            String description,
            String sourceLabel,
            String evidenceSource,
            String dateShared) {}

    public record RequestRecord(
            String website,
            String requestType,
            String status,
            String submittedAt,
            String expectedResponseDate,
            boolean websiteClaimedDeletion,
            boolean userVerified) {}
}
