package com.linklens.userservice.digitalfootprint.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateUserWebsiteAccountRequest(
        @NotBlank String trackedWebsiteId,
        String accountIdentifier,
        String accountCreationDate,
        String accountCategory,
        String sharedInformation,
        String accountStatus,
        String discoverySource,
        Boolean confirmed
) {}
