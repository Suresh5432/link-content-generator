package com.linklens.userservice.digitalfootprint.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterBrowserIntegrationRequest(
        @NotBlank String installationId,
        String browserName,
        String extensionVersion,
        String deviceLabel
) {}
