package com.linklens.summarizationservice.service.ai;

public interface AiProvider {

    AiResponse generateSummary(AiRequest request);

    default String generateText(AiRequest request) {
        return generateSummary(request).summary();
    }

    String getProviderName();

    String getModelName();
}
