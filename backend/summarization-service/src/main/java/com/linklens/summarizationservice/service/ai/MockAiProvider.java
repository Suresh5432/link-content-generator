package com.linklens.summarizationservice.service.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(
        name="summarization.ai.provider",
        havingValue = "mock"
)
public class MockAiProvider implements AiProvider {

    @Override
    public AiResponse generateSummary(AiRequest request) {

        String content=request.content();

        String summary= content.length()>300
                        ? content.substring(0,300)+"..."
                        :content;

        List<String> takeaways=List.of(
                "The webpage discusses : "+request.title(),
                "The content contains : "+content.split("\\s+").length+ " words.",
                "This summary was generated using the mock provider"
        );
        return new AiResponse(
                summary,
                takeaways,
                getProviderName(),
                getModelName(),
                null,
                null
        );
    }

    @Override
    public String generateText(AiRequest request) {
        String content = request.content() == null ? "" : request.content().trim();
        if (content.isBlank()) {
            throw new IllegalStateException("The mock provider requires a non-empty request");
        }
        if (content.contains("Subject:")) {
            return "Subject: Request regarding your personal data\n\n"
                    + "This is a deterministic mock draft for local development. Review and edit it before use.";
        }
        return "This is a deterministic mock answer. The records above are the only source of truth.";
    }

    @Override
    public String getProviderName() {
        return "MOCK";
    }

    @Override
    public String getModelName() {
        return "mock-v1";
    }
}
