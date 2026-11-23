package com.linklens.userservice.digitalfootprint.dto;

import jakarta.validation.constraints.NotBlank;

public record CreatePrivacyRequest(
        String trackedWebsiteId,
        @NotBlank String requestType,
        String requestMessage,
        String privacyContactUrl,
        String privacyContactEmail,
        String dataCategories,
        String preferredContact,
        String legalBasis
) {}
