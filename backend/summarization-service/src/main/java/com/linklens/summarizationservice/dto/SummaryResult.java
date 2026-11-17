package com.linklens.summarizationservice.dto;

import java.util.List;

public record SummaryResult(
        String summary,
        List<String> keyTakeaways,
        int readingTimeMinutes,
        int wordCount,
        String aiProvider,
        String aiModel
) {
}
