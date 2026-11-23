package com.linklens.userservice.digitalfootprint.event;

import java.util.UUID;

public record PrivacyReminderDueEvent(
        UUID requestId,
        String userId,
        String requestType,
        String status
) {}
