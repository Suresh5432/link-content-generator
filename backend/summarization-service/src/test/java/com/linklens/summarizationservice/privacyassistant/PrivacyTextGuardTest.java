package com.linklens.summarizationservice.privacyassistant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PrivacyTextGuardTest {

    @Test
    void rejectsSecretsAndLongDigitSequences() {
        assertThrows(IllegalArgumentException.class,
                () -> PrivacyTextGuard.rejectSecrets("password: hunter2", "context"));
        assertThrows(IllegalArgumentException.class,
                () -> PrivacyTextGuard.rejectSecrets("card number: 4111111111111111", "context"));
        assertThrows(IllegalArgumentException.class,
                () -> PrivacyTextGuard.rejectSecrets("4111111111111111", "context"));
    }

    @Test
    void allowsCategoryLevelDescriptions() {
        assertEquals("email, address", PrivacyTextGuard.rejectSecrets("email, address", "categories"));
    }

    @Test
    void cleansFencesAndRejectsEmptyOutput() {
        assertEquals("Subject: Deletion request",
                PrivacyTextGuard.cleanOutput("```text\nSubject: Deletion request\n```"));
        assertThrows(IllegalStateException.class, () -> PrivacyTextGuard.cleanOutput("   "));
    }
}
