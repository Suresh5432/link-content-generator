package com.linklens.userservice.digitalfootprint.service;

import com.linklens.userservice.digitalfootprint.exception.InvalidRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Component
public class ConnectedEmailTokenCipher {
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private final String base64Key;
    private final SecureRandom secureRandom = new SecureRandom();

    public ConnectedEmailTokenCipher(
            @Value("${digital-footprint.connected-email.encryption-key:}") String base64Key
    ) {
        this.base64Key = base64Key;
    }

    public boolean isConfigured() {
        try {
            return decodedKey().length == 32;
        } catch (IllegalArgumentException | InvalidRequestException exception) {
            return false;
        }
    }

    public EncryptedValue encrypt(String value) {
        byte[] iv = new byte[IV_LENGTH];
        secureRandom.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(decodedKey(), "AES"), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new EncryptedValue(cipher.doFinal(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)), iv);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not encrypt connected-email credentials", exception);
        }
    }

    public String decrypt(byte[] ciphertext, byte[] iv) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(decodedKey(), "AES"), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(ciphertext), java.nio.charset.StandardCharsets.UTF_8);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not decrypt connected-email credentials", exception);
        }
    }

    private byte[] decodedKey() {
        if (base64Key == null || base64Key.isBlank()) {
            throw new InvalidRequestException("Connected email discovery requires a configured 32-byte encryption key.");
        }
        byte[] key;
        try {
            key = Base64.getDecoder().decode(base64Key);
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException("Connected email encryption key must be Base64 encoded.");
        }
        if (key.length != 32) {
            Arrays.fill(key, (byte) 0);
            throw new InvalidRequestException("Connected email encryption key must decode to exactly 32 bytes.");
        }
        return key;
    }

    public record EncryptedValue(byte[] ciphertext, byte[] iv) {}
}
