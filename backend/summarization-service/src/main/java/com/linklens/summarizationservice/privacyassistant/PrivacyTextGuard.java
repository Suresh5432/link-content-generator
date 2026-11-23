package com.linklens.summarizationservice.privacyassistant;

import java.util.Locale;

/**
 * Defence in depth for the opt-in AI privacy features: never send raw secrets
 * to a provider, and never return generated text that looks like it contains one.
 */
public final class PrivacyTextGuard {

    private PrivacyTextGuard() {
    }

    public static String rejectSecrets(String text, String field) {
        if (text == null) return "";
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("password:") || lower.contains("passwd:")
                || lower.contains("cvv:") || lower.contains("card number:")
                || lower.contains("api key:") || lower.contains("secret key:")
                || lower.contains("bearer ") || lower.contains("session token:")) {
            throw new IllegalArgumentException(
                    field + " must not contain passwords, tokens, card numbers or other raw secrets");
        }
        if (text.matches(".*\\d{13,}.*")) {
            throw new IllegalArgumentException(
                    field + " must not contain full card or identity numbers; use category-level data only");
        }
        return text;
    }

    public static String cleanOutput(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalStateException("The AI provider returned an empty response");
        }
        String cleaned = text.trim()
                .replaceFirst("^```[a-zA-Z]*\\s*", "")
                .replaceFirst("\\s*```$", "")
                .trim();
        if (cleaned.isBlank()) {
            throw new IllegalStateException("The AI provider returned an empty response");
        }
        return rejectSecrets(cleaned, "AI output");
    }
}
