package com.linklens.userservice.digitalfootprint.dto;

public record ConnectedEmailStatusResponse(
        boolean configured,
        String provider,
        String requiredScope,
        String limitation
) {}
