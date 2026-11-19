package com.linklens.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(

        @Size(min = 1, max = 80,
                message = "Display name must contain 1-80 characters")
        String displayName,

        @Size(max = 500,
                message = "Bio cannot exceed 500 characters")
        String bio,

        @Size(max = 2048, message = "Avatar URL is too long")
        @Pattern(
                regexp = "https?://.*",
                message = "Avatar URL must use HTTP or HTTPS"
        )
        String avatarUrl


) {

}
