package com.linklens.userservice.digitalfootprint.domain;

import java.util.Locale;

public enum PrivacyDataSource {
    USER_CONFIRMED,
    OBSERVED_ON_WEBSITE,
    DECLARED_IN_POLICY,
    INFERRED_BY_AI,
    UNKNOWN;

    public static PrivacyDataSource parse(String value) {
        if (value == null || value.isBlank()) return USER_CONFIRMED;
        return valueOf(value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_'));
    }
}
