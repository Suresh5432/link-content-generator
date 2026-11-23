package com.linklens.userservice.digitalfootprint.dto;

import java.util.List;

public record ConnectedEmailScanResponse(
        int messagesReviewed,
        int possibleAccountsFound,
        List<ConnectedEmailDiscoveryResponse> discoveries
) {}
