package com.linkcontext.contracts.events;

import java.time.Instant;
import java.util.UUID;

public record UserFootprintDeletedEvent(
        UUID eventId,
        String userId,
        Instant deletedAt,
        String scope,
        int version
) {}
