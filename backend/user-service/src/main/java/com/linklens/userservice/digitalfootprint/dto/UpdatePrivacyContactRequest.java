package com.linklens.userservice.digitalfootprint.dto;

public record UpdatePrivacyContactRequest(
        String privacyPolicyUrl,
        String deletionPolicyUrl,
        String accessRequestUrl,
        String accountDeletionUrl,
        String cookieManagementUrl,
        String consentManagementUrl,
        String privacyContactEmail,
        String dpoContact,
        String sourceUrls
) {}
