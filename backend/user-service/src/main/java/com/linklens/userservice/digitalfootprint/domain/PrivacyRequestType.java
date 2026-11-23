package com.linklens.userservice.digitalfootprint.domain;

import java.util.Locale;

public enum PrivacyRequestType {
    ACCESS,
    DELETION,
    ACCOUNT_DELETION,
    CORRECTION,
    MARKETING_OPT_OUT,
    TRACKING_OPT_OUT,
    COOKIE_REMOVAL,
    CONSENT_WITHDRAWAL,
    PORTABILITY;

    public static PrivacyRequestType parse(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("requestType is required");
        return valueOf(value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_'));
    }
}
