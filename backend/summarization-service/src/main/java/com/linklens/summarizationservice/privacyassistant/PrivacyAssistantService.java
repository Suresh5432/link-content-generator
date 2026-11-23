package com.linklens.summarizationservice.privacyassistant;

import com.linklens.summarizationservice.service.ai.AiProvider;
import com.linklens.summarizationservice.service.ai.AiRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PrivacyAssistantService {

    private static final int MAX_QUESTION = 2000;
    private static final int MAX_CONTEXT = 20000;

    private final AiProvider aiProvider;

    public String answer(String question, String context) {
        String safeQuestion = question == null ? "" : question.trim();
        String safeContext = context == null ? "" : context.trim();
        if (safeQuestion.isEmpty()) throw new IllegalArgumentException("question is required");
        if (safeQuestion.length() > MAX_QUESTION) throw new IllegalArgumentException("question is too long");
        if (safeContext.length() > MAX_CONTEXT) throw new IllegalArgumentException("context is too large");
        return aiProvider.generateSummary(new AiRequest("Privacy assistant", buildPrompt(safeQuestion, safeContext), "en")).summary();
    }

    private String buildPrompt(String question, String context) {
        return """
                You are a privacy assistant inside the user's own "My Digital Footprint" dashboard.

                Rules:
                - Answer only from the RECORDS section. Never invent a website, account, record, date, email or request.
                - If the records do not contain the answer, say that the information is not recorded.
                - Preserve evidence labels. Clearly distinguish:
                  USER_CONFIRMED (the user confirmed it),
                  OBSERVED_ON_WEBSITE (observed from a website/analysis, not proof of transmission),
                  DECLARED_IN_POLICY (declared in a privacy policy),
                  INFERRED_BY_AI (uncertain AI inference, never a confirmed fact),
                  UNKNOWN.
                - Never claim that deleting a browser cookie deletes server-side account data.
                - Never claim a request is completed unless user_verified is true; a website's claim is evidence, not proof.
                - Do not ask for or repeat passwords, tokens, card numbers, identity numbers or other raw secrets.
                - Be concise and practical. Prefer short lists and name the website for each item.

                RECORDS:
                %s

                QUESTION:
                %s
                """.formatted(context.isBlank() ? "No records were provided." : context, question);
    }
}
