package com.linklens.userservice.digitalfootprint.dto;

public record UpdatePrivacyRequestRequest(
        String requestMessage,
        String privacyContactUrl,
        String privacyContactEmail,
        String dataCategories,
        String preferredContact,
        String legalBasis,
        String submissionMethod,
        String submissionEvidence,
        String expectedResponseDate
) {}
