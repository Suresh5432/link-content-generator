package com.linkcontext.contracts.events;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Public privacy resources discovered by Website Intelligence. Findings carry
 * their source URLs as evidence and must be treated as unverified hints.
 */
public record PrivacyDiscoveryDiscoveredEvent(
        UUID eventId,
        UUID analysisId,
        String userId,
        String domain,
        String websiteUrl,
        List<Finding> findings,
        Instant occurredAt,
        int version) {

    public record Finding(
            String type,
            String name,
            String sourceUrl,
            String description,
            String confidence) {}
}
