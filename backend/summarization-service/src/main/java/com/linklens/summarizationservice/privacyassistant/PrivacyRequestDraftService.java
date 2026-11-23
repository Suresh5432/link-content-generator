package com.linklens.summarizationservice.privacyassistant;

import com.linklens.summarizationservice.service.ai.AiProvider;
import com.linklens.summarizationservice.service.ai.AiRequest;
import com.linklens.summarizationservice.service.ai.ResponseFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PrivacyRequestDraftService {

    private final AiProvider aiProvider;

    public String draft(String requestType, String websiteName, String dataCategories) {
        String type = normalize(requestType, "privacy request", 40);
        String website = normalize(websiteName, "the website", 300);
        String categories = dataCategories == null || dataCategories.isBlank()
                ? "not specified (do not invent any)"
                : trimTo(dataCategories, 1000);
        PrivacyTextGuard.rejectSecrets(type, "request type");
        PrivacyTextGuard.rejectSecrets(website, "website name");
        PrivacyTextGuard.rejectSecrets(categories, "data categories");
        return PrivacyTextGuard.cleanOutput(aiProvider.generateText(
                new AiRequest("Privacy request draft", buildPrompt(type, website, categories), "en", ResponseFormat.PLAIN_TEXT)));
    }

    private String buildPrompt(String requestType, String websiteName, String dataCategories) {
        return """
                You draft a privacy request message for a user to review and send themselves.

                Return ONLY the finished message, including a short "Subject:" line followed by a blank line and the body.
                Do not add commentary, markdown fences, explanations or placeholders like [your name].

                Facts you may use:
                - Request type: %s
                - Website: %s
                - Data categories the user says they shared: %s

                Rules:
                - Be polite, clear and professional. 150-220 words.
                - Never invent account identifiers, dates, email addresses, legal citations or website policies.
                - Never include passwords, tokens, session identifiers, full card numbers or identity numbers.
                - Ask for the website's identity-verification steps.
                - Ask whether any information is retained for legal, regulatory or operational reasons.
                - Ask for confirmation once the request is processed.
                - Do not claim the request has already been sent.
                """.formatted(requestType, websiteName, dataCategories);
    }

    private static String normalize(String value, String fallback, int max) {
        if (value == null || value.isBlank()) return fallback;
        return trimTo(value.trim(), max);
    }

    private static String trimTo(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
