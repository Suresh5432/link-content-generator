package com.linklens.userservice.digitalfootprint.service;

import com.linklens.userservice.digitalfootprint.entity.ConnectedEmailOAuthState;
import com.linklens.userservice.digitalfootprint.exception.InvalidRequestException;
import com.linklens.userservice.digitalfootprint.repo.ConnectedEmailOAuthStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ConnectedEmailOAuthStateStore {
    private final ConnectedEmailOAuthStateRepository repository;
    private final ConnectedEmailTokenCipher cipher;

    @Transactional
    public void save(String stateHash, String userId, String verifier, Instant expiresAt) {
        repository.deleteAllByExpiresAtBefore(Instant.now());
        ConnectedEmailTokenCipher.EncryptedValue encryptedVerifier = cipher.encrypt(verifier);
        repository.save(ConnectedEmailOAuthState.builder()
                .stateHash(stateHash)
                .userId(userId)
                .verifierCiphertext(encryptedVerifier.ciphertext())
                .verifierIv(encryptedVerifier.iv())
                .expiresAt(expiresAt)
                .createdAt(Instant.now())
                .build());
    }

    @Transactional
    public ConsumedState consume(String stateHash) {
        ConnectedEmailOAuthState state = repository.findByStateHashForUpdate(stateHash)
                .orElseThrow(() -> new InvalidRequestException("OAuth state is invalid or has already been used."));
        repository.delete(state);
        if (state.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidRequestException("OAuth state expired. Start the connection again.");
        }
        return new ConsumedState(state.getUserId(),
                cipher.decrypt(state.getVerifierCiphertext(), state.getVerifierIv()));
    }

    public record ConsumedState(String userId, String verifier) {}
}
