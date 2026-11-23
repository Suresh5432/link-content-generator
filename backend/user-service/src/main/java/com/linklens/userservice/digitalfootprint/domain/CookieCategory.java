package com.linklens.userservice.digitalfootprint.domain;

import java.util.Locale;

public enum CookieCategory {
    ESSENTIAL,
    FUNCTIONAL,
    ANALYTICS,
    ADVERTISING,
    SOCIAL_MEDIA,
    UNKNOWN;

    public static CookieCategory parse(String value) {
        if (value == null || value.isBlank()) return UNKNOWN;
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_'));
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
