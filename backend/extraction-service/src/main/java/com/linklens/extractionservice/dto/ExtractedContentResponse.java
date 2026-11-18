package com.linklens.extractionservice.dto;

import java.time.Instant;
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
