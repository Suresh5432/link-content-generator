package com.linklens.extractionservice.websiteanalysis.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PrivacyDiscoveryClassifierTest {

    @Test
    void classifiesCommonPrivacyLinks() {
        assertEquals("PRIVACY_POLICY", PrivacyDiscoveryClassifier.classifyLink("Privacy policy", "https://example.com/privacy"));
        assertEquals("DATA_DELETION_PAGE", PrivacyDiscoveryClassifier.classifyLink("Delete my data", "https://example.com/delete-my-data"));
        assertEquals("ACCOUNT_DELETION_PAGE", PrivacyDiscoveryClassifier.classifyLink("Delete account", "https://example.com/account-deletion"));
        assertEquals("DATA_ACCESS_PAGE", PrivacyDiscoveryClassifier.classifyLink("Access my data", "https://example.com/data-access"));
        assertEquals("COOKIE_MANAGEMENT_PAGE", PrivacyDiscoveryClassifier.classifyLink("Cookie settings", "https://example.com/cookies"));
        assertEquals("CONSENT_MANAGEMENT_PAGE", PrivacyDiscoveryClassifier.classifyLink("Do not sell or share", "https://example.com/opt-out"));
        assertEquals("DPO_CONTACT", PrivacyDiscoveryClassifier.classifyLink("Data protection officer", "https://example.com/dpo"));
    }

    @Test
    void ignoresUnrelatedLinks() {
        assertNull(PrivacyDiscoveryClassifier.classifyLink("Our products", "https://example.com/products"));
        assertNull(PrivacyDiscoveryClassifier.classifyLink("", ""));
    }

    @Test
    void extractsOnlyPrivacyRelatedEmails() {
        String text = "Contact privacy@example.com or support@example.com. DPO: dpo@example.org.";
        List<String> emails = PrivacyDiscoveryClassifier.privacyEmails(text);
        assertEquals(List.of("privacy@example.com", "dpo@example.org"), emails);
    }

    @Test
    void recognisesPrivacyEmails() {
        assertTrue(PrivacyDiscoveryClassifier.isPrivacyEmail("dataprotection@example.com"));
        assertTrue(PrivacyDiscoveryClassifier.isPrivacyEmail("dpo@example.com"));
        assertFalse(PrivacyDiscoveryClassifier.isPrivacyEmail("sales@example.com"));
        assertFalse(PrivacyDiscoveryClassifier.isPrivacyEmail(null));
    }
}
