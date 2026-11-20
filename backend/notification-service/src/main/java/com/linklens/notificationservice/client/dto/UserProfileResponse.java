package com.linklens.notificationservice.client.dto;

import java.time.Instant;

public record UserProfileResponse(
        String userId,
        String displayName,
        String email,
        String bio,
        String avatarUrl,
        Instant createdAt,
        Instant updatedAt
) {
}
