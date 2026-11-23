package com.linkcontext.contracts.events;

import java.time.Instant;
import java.util.UUID;

public record PrivacyRequestStatusChangedEvent(
        UUID eventId,
        UUID requestId,
        String userId,
        String requestType,
        String previousStatus,
        String newStatus,
        Instant occurredAt,
        int version
) {}
