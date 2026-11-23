package com.linklens.userservice.digitalfootprint.service;

import com.linklens.userservice.digitalfootprint.exception.InvalidRequestException;

import java.util.Locale;

/**
 * Defence-in-depth: footprint records are category-level. Reject material that
 * looks like a raw secret so it is never persisted, sent to AI, or logged.
 */
final class SensitiveDataGuard {
    static String cleanOptionalEmail(String email) {
        if (email == null || email.isBlank()) return null;
        String trimmed = email.trim();
        rejectSecretMaterial(trimmed);
        if (!trimmed.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new InvalidRequestException("accountEmail is not a valid email address");
        }
        return trimmed.toLowerCase(java.util.Locale.ROOT);
    }

    private SensitiveDataGuard() {}

    static void rejectSecretMaterial(String text) {
        if (text == null) return;
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("cvv") || lower.contains("card number:") || lower.contains("password:")
                || lower.contains("api key:") || lower.contains("bearer ")) {
            throw new InvalidRequestException("Do not store passwords, card numbers, tokens or other raw secrets; record the category only");
        }
        if (text.matches(".*\\d{13,}.*")) {
            throw new InvalidRequestException("Do not store full card or identity numbers; record the category only");
        }
    }

    static void rejectSecretMaterial(String dataType, String description) {
        String type = dataType == null ? "" : dataType.trim().toUpperCase(Locale.ROOT);
        if (type.equals("PASSWORD") || type.equals("CARD_NUMBER") || type.equals("CVV")
                || type.equals("SESSION_TOKEN") || type.equals("ACCESS_TOKEN")) {
            throw new InvalidRequestException("Store the category only; do not store passwords, card numbers, tokens or session secrets");
        }
        rejectSecretMaterial(description);
    }
}
