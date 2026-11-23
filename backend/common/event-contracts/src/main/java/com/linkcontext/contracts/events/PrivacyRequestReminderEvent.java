package com.linkcontext.contracts.events;

import java.time.Instant;
import java.util.UUID;

public record PrivacyRequestReminderEvent(
        UUID eventId,
        UUID requestId,
        String userId,
        String requestType,
        String status,
        Instant occurredAt,
        int version
) {}
