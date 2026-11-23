package com.linklens.userservice.digitalfootprint.dto;

public record UpdatePrivacyPreferencesRequest(
        Boolean analyticsCookies,
        Boolean advertisingCookies,
        Boolean marketingCommunications,
        Boolean thirdPartyTracking,
        Boolean personalizedAdvertising,
        Boolean dataSharing
) {}
