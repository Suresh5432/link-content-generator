package com.linklens.extractionservice.websiteanalysis.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Heuristic classifier for publicly linked privacy resources. This is evidence for
 * the user to verify, never a legal guarantee. Kept separate for testability.
 */
final class PrivacyDiscoveryClassifier {
    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");

    private PrivacyDiscoveryClassifier() {}

    static String classifyLink(String text, String href) {
        String probe = ((text == null ? "" : text) + " " + (href == null ? "" : href)).toLowerCase(Locale.ROOT);
        if (probe.contains("dpo") || probe.contains("data protection officer")) return "DPO_CONTACT";
        if (probe.contains("delete account") || probe.contains("account deletion") || probe.contains("account-deletion")) return "ACCOUNT_DELETION_PAGE";
        if (probe.contains("delete my data") || probe.contains("data deletion") || probe.contains("data-deletion") || probe.contains("delete-my-data")) return "DATA_DELETION_PAGE";
        if (probe.contains("access my data") || probe.contains("data access") || probe.contains("data-access") || probe.contains("subject access")) return "DATA_ACCESS_PAGE";
        if (probe.contains("cookie settings") || probe.contains("cookie preferences") || probe.contains("cookie-policy") || probe.contains("cookie management")) return "COOKIE_MANAGEMENT_PAGE";
        if (probe.contains("consent") || probe.contains("opt out") || probe.contains("opt-out") || probe.contains("do not sell")) return "CONSENT_MANAGEMENT_PAGE";
        if (probe.contains("privacy policy") || probe.contains("privacy-policy") || probe.contains("/privacy")) return "PRIVACY_POLICY";
        return null;
    }

    static boolean isPrivacyEmail(String email) {
        if (email == null || !email.contains("@")) return false;
        String lower = email.toLowerCase(Locale.ROOT);
        return lower.contains("privacy") || lower.contains("dpo") || lower.contains("dataprotection") || lower.contains("data-protection");
    }

    static List<String> privacyEmails(String text) {
        Set<String> found = new LinkedHashSet<>();
        if (text == null) return List.of();
        Matcher matcher = EMAIL.matcher(text);
        while (matcher.find() && found.size() < 10) {
            String email = matcher.group();
            if (isPrivacyEmail(email)) found.add(email);
        }
        return List.copyOf(found);
    }
}
