package com.linklens.summarizationservice.privacyassistant;

import com.linklens.summarizationservice.service.ai.AiProvider;
import com.linklens.summarizationservice.service.ai.AiRequest;
import com.linklens.summarizationservice.service.ai.ResponseFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class PrivacyAssistantService {

    private static final int MAX_QUESTION = 2000;

    private final AiProvider aiProvider;
    private final FootprintContextClient contextClient;
    private final WebsiteAnalysisContextClient analysisContextClient;
    private final ObjectMapper objectMapper;

    public String answer(String question, String userId) {
        String safeQuestion = question == null ? "" : question.trim();
        if (safeQuestion.isEmpty()) throw new IllegalArgumentException("question is required");
        if (safeQuestion.length() > MAX_QUESTION) throw new IllegalArgumentException("question is too long");
        if (userId == null || userId.isBlank()) throw new IllegalArgumentException("authenticated user is required");
        PrivacyTextGuard.rejectSecrets(safeQuestion, "question");

        String footprint = serialize(contextClient.getContext(userId), "privacy records");
        String analysis = serialize(analysisContextClient.getContext(userId), "website analysis evidence");
        return PrivacyTextGuard.cleanOutput(aiProvider.generateText(
                new AiRequest("Privacy assistant", buildPrompt(safeQuestion, footprint, analysis), "en", ResponseFormat.PLAIN_TEXT)));
    }

    private String serialize(Object snapshot, String label) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to prepare the user's " + label, exception);
        }
    }

    private String buildPrompt(String question, String footprint, String analysis) {
        return """
                You are a privacy assistant inside the user's own "My Digital Footprint" dashboard.

                Rules:
                - Answer only from the FOOTPRINT_RECORDS and WEBSITE_ANALYSIS_EVIDENCE sections. Never invent a website, account, record, date, email or request.
                - If the records do not contain the answer, say that the information is not recorded.
                - Preserve evidence labels. Clearly distinguish:
                  USER_CONFIRMED (the user confirmed it),
                  OBSERVED_ON_WEBSITE (observed from a website/analysis, not proof of transmission),
                  DECLARED_IN_POLICY (declared in a privacy policy),
                  INFERRED_BY_AI (uncertain AI inference, never a confirmed fact),
                  UNKNOWN.
                - Website analysis reports are inspection evidence owned by the analysis system. They can support observed/declared signals, but they are not proof of account creation, data transmission, or server-side deletion unless corroborated by user-owned footprint records.
                - Never claim that deleting a browser cookie deletes server-side account data.
                - Never claim a request is completed unless user_verified is true; a website's claim is evidence, not proof.
                - Do not ask for or repeat passwords, tokens, card numbers, identity numbers or other raw secrets.
                - Be concise and practical. Prefer short lists and name the website for each item.

                FOOTPRINT_RECORDS:
                %s

                WEBSITE_ANALYSIS_EVIDENCE:
                %s

                QUESTION:
                %s
                """.formatted(footprint, analysis, question);
    }
}
