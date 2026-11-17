package com.linklens.summarizationservice.dto;

import java.util.UUID;

public record ExtractedContentResponse(
        UUID contentId,
        UUID linkId,
        String userId,
        String sourceUrl,
        String title,
        String description,
        String language,
        String extractedText,
        int wordCount
) {
}
