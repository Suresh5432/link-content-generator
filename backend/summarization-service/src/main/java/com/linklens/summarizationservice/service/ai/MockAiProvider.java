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
    public String getProviderName() {
        return "MOCK";
    }

    @Override
    public String getModelName() {
        return "mock-v1";
    }
}
