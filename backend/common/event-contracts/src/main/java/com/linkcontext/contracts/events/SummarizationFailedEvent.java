package com.linkcontext.contracts.events;

import java.time.Instant;
import java.util.UUID;

public record SummarizationFailedEvent(
        UUID eventId,
        UUID linkId,
        String userId,
        String failureCode,
        boolean retryable,
        Instant occurredAt,
        int version
) {
}
