package com.linklens.userservice.digitalfootprint.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePersonalDataRecordRequest(
        String trackedWebsiteId,
        @NotBlank @Size(max = 100) String dataCategory,
        @NotBlank @Size(max = 100) String dataType,
        String description,
        String source,
        String dateShared,
        String evidenceSource
) {}
