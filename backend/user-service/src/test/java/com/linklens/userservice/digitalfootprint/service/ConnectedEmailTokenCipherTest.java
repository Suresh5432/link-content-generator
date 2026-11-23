package com.linklens.userservice.digitalfootprint.service;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class ConnectedEmailTokenCipherTest {
    @Test
    void encryptsAndDecryptsProviderCredentials() {
        String key = Base64.getEncoder().encodeToString(new byte[32]);
        ConnectedEmailTokenCipher cipher = new ConnectedEmailTokenCipher(key);

        ConnectedEmailTokenCipher.EncryptedValue encrypted = cipher.encrypt("synthetic-refresh-token");

        assertTrue(cipher.isConfigured());
        assertNotEquals("synthetic-refresh-token", new String(encrypted.ciphertext()));
        assertNotEquals(new String(encrypted.iv()), new String(cipher.encrypt("synthetic-refresh-token").iv()));
        assertEquals("synthetic-refresh-token", cipher.decrypt(encrypted.ciphertext(), encrypted.iv()));
    }

    @Test
    void reportsMissingOrInvalidEncryptionKeyAsNotConfigured() {
        assertFalse(new ConnectedEmailTokenCipher("").isConfigured());
        assertFalse(new ConnectedEmailTokenCipher(Base64.getEncoder().encodeToString(new byte[16])).isConfigured());
        assertThrows(IllegalStateException.class, () -> new ConnectedEmailTokenCipher("").encrypt("secret"));
    }
}
