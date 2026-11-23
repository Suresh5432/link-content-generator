package com.linkcontext.contracts.events;

import java.time.Instant;
import java.util.UUID;

public record WebsiteAnalysisCompletedEvent(
        UUID eventId,
        UUID analysisId,
        String userId,
        Instant occurredAt,
        int version
) {}
