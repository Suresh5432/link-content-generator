package com.linkcontext.contracts.events;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SummaryGeneratedEvent(
        UUID eventId,
        UUID linkId,
        String userId,
        String summary,
        List<String> keyTakeaways,
        int readingTimeMinutes,
        Instant occurredAt,
        int version
){}
