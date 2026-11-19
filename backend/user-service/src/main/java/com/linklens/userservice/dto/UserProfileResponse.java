package com.linklens.userservice.dto;

import java.time.Instant;

public record UserProfileResponse(
        String userId,
        String displayName,
        String bio,
        String avatarUrl,
        Instant createdAt,
        Instant updatedAt
)
{}
