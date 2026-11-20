package com.linklens.notificationservice.client.dto;

public record UserProfileResponse(
        String userId,
        String displayName,
        String email
) {
}
