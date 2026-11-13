package com.linkcontext.contracts.events;

import java.time.Instant;
import java.util.UUID;

public record LinkSubmittedEvent(
    UUID eventId,
    UUID linkId,
    String userId,
    String url,
    Instant occurredAt,
    int version
    ){}
