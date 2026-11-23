package com.linklens.userservice.digitalfootprint.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateEvidenceRequest(
        @NotBlank String type,
        String description,
        String referenceUrl
) {}
