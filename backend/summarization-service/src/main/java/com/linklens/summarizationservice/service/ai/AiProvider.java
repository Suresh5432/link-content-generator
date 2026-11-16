package com.linklens.summarizationservice.service.ai;

public interface AiProvider {

    AiResponse generateSummary(AiRequest request);

    String getProviderName();

    String getModelName();
}
