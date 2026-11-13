package com.linkcontext.contracts.events;

import java.time.Instant;
import java.util.UUID;

public record ExtractionFailedEvent(
        UUID eventId,
        UUID linkId,
        String userId,
        String failureCode,
        boolean retryable,
        Instant occurredAt,
        int version
) {
}
