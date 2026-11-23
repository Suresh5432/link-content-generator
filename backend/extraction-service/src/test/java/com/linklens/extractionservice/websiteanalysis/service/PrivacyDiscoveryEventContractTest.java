package com.linklens.extractionservice.websiteanalysis.service;

import com.linkcontext.contracts.EventRoutingKey;
import com.linkcontext.contracts.events.PrivacyDiscoveryDiscoveredEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PrivacyDiscoveryEventContractTest {

    @Test
    void versionedEventCarriesFindingsWithSourceUrls() {
        UUID analysisId = UUID.randomUUID();
        PrivacyDiscoveryDiscoveredEvent event = new PrivacyDiscoveryDiscoveredEvent(
                UUID.randomUUID(), analysisId, "user-1", "example.com", "https://example.com",
                List.of(new PrivacyDiscoveryDiscoveredEvent.Finding("PRIVACY_POLICY", "Privacy policy",
                        "https://example.com/privacy", "Publicly linked", "MEDIUM")),
                Instant.now(), 1);

        assertEquals(1, event.version());
        assertEquals("user-1", event.userId());
        assertEquals("example.com", event.domain());
        assertEquals("https://example.com/privacy", event.findings().get(0).sourceUrl());
        assertEquals("privacy.discovery.discovered.v1", EventRoutingKey.PRIVACY_DISCOVERY_DISCOVERED);
    }
}
