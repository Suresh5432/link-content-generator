package com.linklens.summarizationservice.service.ai;

import java.util.List;

public record AiResponse(
        String summary,
        List<String> keyTakeaways,
        String provider,
        String model,
        Integer inputTokens,
        Integer outputTokens
) {}
