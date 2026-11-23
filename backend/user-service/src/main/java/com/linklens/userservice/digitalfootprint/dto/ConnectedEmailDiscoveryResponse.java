package com.linklens.userservice.digitalfootprint.dto;

import com.linklens.userservice.digitalfootprint.entity.ConnectedEmailDiscovery;

import java.time.Instant;
import java.util.UUID;

public record ConnectedEmailDiscoveryResponse(
        UUID id,
        UUID connectedAccountId,
        String provider,
        String domain,
        String evidenceType,
        String evidenceSummary,
        Instant firstSeenAt,
        Instant lastSeenAt,
        String confidence,
        boolean confirmedByUser,
        boolean dismissedByUser
) {
    public static ConnectedEmailDiscoveryResponse from(ConnectedEmailDiscovery discovery) {
        return new ConnectedEmailDiscoveryResponse(
                discovery.getId(), discovery.getConnectedAccountId(), discovery.getProvider(),
                discovery.getDomain(), discovery.getEvidenceType(), discovery.getEvidenceSource(),
                discovery.getFirstSeenAt(), discovery.getLastSeenAt(), discovery.getConfidence(),
                discovery.isConfirmedByUser(), discovery.isDismissedByUser()
        );
    }
}
