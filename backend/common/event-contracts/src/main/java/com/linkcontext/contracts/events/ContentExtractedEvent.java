package com.linkcontext.contracts.events;

import java.time.Instant;
import java.util.UUID;

public record ContentExtractedEvent(
        UUID eventId,
        UUID linkId,
        UUID contentId,
        String userId,
        String title,
        String canonicalUrl,
        String contentType,
        int wordCount,
        Instant occurredAt,
        int version
) {
}
