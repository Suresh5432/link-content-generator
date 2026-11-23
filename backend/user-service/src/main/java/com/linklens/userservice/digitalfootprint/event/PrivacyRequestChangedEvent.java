package com.linklens.userservice.digitalfootprint.event;

import java.util.UUID;

public record PrivacyRequestChangedEvent(
        UUID requestId,
        String userId,
        String requestType,
        String previousStatus,
        String newStatus
) {}
