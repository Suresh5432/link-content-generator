package com.linklens.userservice.digitalfootprint.domain;

import java.util.Locale;

public enum EvidenceType {
    SUBMISSION,
    RESPONSE,
    VERIFICATION,
    OTHER;

    public static EvidenceType parse(String value) {
        if (value == null || value.isBlank()) return OTHER;
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return OTHER;
        }
    }
}
