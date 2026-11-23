package com.linklens.userservice.digitalfootprint.dto;

public record UpdateUserWebsiteAccountRequest(
        String accountIdentifier,
        String accountCreationDate,
        String accountCategory,
        String sharedInformation,
        String accountStatus,
        Boolean confirmed
) {}
