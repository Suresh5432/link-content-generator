package com.linklens.summarizationservice.dto;

import com.linklens.summarizationservice.entity.Summary;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SummaryResponse(
        UUID id,
        UUID linkId,
        UUID contentId,
        String title,
        String summary,
        List<String> keyTakeaways,
        int readingTimeMinutes,
        int wordCount,
        String aiProvider,
        String aiModel,
        Instant createdAt,
        Instant updatedAt
) {
    public static SummaryResponse from(Summary summary) {
        return new SummaryResponse(
                summary.getId(),
                summary.getLinkId(),
                summary.getContentId(),
                summary.getTitle(),
                summary.getSummary(),
                summary.getKeyTakeaways() == null ? List.of() : List.copyOf(summary.getKeyTakeaways()),
                summary.getReadingTimeMinutes(),
                summary.getWordCount(),
                summary.getAiProvider(),
                summary.getAiModel(),
                summary.getCreatedAt(),
                summary.getUpdatedAt());
    }
}
