package com.linklens.extractionservice.dto;

import java.time.Instant;
import java.util.UUID;

public record ExtractedContentResponse(
        UUID id,
        UUID linkId,
        String userId,
        String sourceUrl,
        String canonicalUrl,
        String title,
        String description,
        String contentType,
        String language,
        String extractedText,
        int wordCount,
        Instant fetchedAt
) {
}
