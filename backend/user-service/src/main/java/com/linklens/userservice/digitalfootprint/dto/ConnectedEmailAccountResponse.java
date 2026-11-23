package com.linklens.userservice.digitalfootprint.dto;

import com.linklens.userservice.digitalfootprint.entity.ConnectedEmailAccount;

import java.time.Instant;
import java.util.UUID;

public record ConnectedEmailAccountResponse(
        UUID id,
        String provider,
        String accountEmail,
        String status,
        Instant consentedAt,
        Instant lastScanAt,
        Instant createdAt
) {
    public static ConnectedEmailAccountResponse from(ConnectedEmailAccount account) {
        return new ConnectedEmailAccountResponse(
                account.getId(), account.getProvider(), account.getAccountEmail(),
                account.getStatus(), account.getConsentedAt(), account.getLastScanAt(),
                account.getCreatedAt()
        );
    }
}
