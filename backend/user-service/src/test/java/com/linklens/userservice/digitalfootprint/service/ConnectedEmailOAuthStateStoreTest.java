package com.linklens.userservice.digitalfootprint.service;

import com.linklens.userservice.digitalfootprint.entity.ConnectedEmailOAuthState;
import com.linklens.userservice.digitalfootprint.exception.InvalidRequestException;
import com.linklens.userservice.digitalfootprint.repo.ConnectedEmailOAuthStateRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConnectedEmailOAuthStateStoreTest {
    @Test
    void consumesOAuthStateOnceAndDecryptsVerifier() {
        ConnectedEmailOAuthStateRepository repository = mock(ConnectedEmailOAuthStateRepository.class);
        ConnectedEmailTokenCipher cipher = mock(ConnectedEmailTokenCipher.class);
        ConnectedEmailOAuthStateStore store = new ConnectedEmailOAuthStateStore(repository, cipher);
        ConnectedEmailOAuthState state = ConnectedEmailOAuthState.builder()
                .stateHash("hashed-state").userId("user-1").verifierCiphertext(new byte[]{1})
                .verifierIv(new byte[]{2}).expiresAt(Instant.now().plusSeconds(120)).build();
        when(repository.findByStateHashForUpdate("hashed-state")).thenReturn(Optional.of(state));
        when(cipher.decrypt(new byte[]{1}, new byte[]{2})).thenReturn("pkce-verifier");

        var consumed = store.consume("hashed-state");

        assertEquals("user-1", consumed.userId());
        assertEquals("pkce-verifier", consumed.verifier());
        verify(repository).delete(state);
    }

    @Test
    void rejectsUnknownOrReusedState() {
        ConnectedEmailOAuthStateRepository repository = mock(ConnectedEmailOAuthStateRepository.class);
        ConnectedEmailTokenCipher cipher = mock(ConnectedEmailTokenCipher.class);
        ConnectedEmailOAuthStateStore store = new ConnectedEmailOAuthStateStore(repository, cipher);
        when(repository.findByStateHashForUpdate("unknown")).thenReturn(Optional.empty());

        assertThrows(InvalidRequestException.class, () -> store.consume("unknown"));
        verify(repository, never()).delete(any());
        verifyNoInteractions(cipher);
    }

    @Test
    void rejectsExpiredState() {
        ConnectedEmailOAuthStateRepository repository = mock(ConnectedEmailOAuthStateRepository.class);
        ConnectedEmailTokenCipher cipher = mock(ConnectedEmailTokenCipher.class);
        ConnectedEmailOAuthStateStore store = new ConnectedEmailOAuthStateStore(repository, cipher);
        ConnectedEmailOAuthState state = ConnectedEmailOAuthState.builder()
                .stateHash("expired").userId("user-1").verifierCiphertext(new byte[]{1})
                .verifierIv(new byte[]{2}).expiresAt(Instant.now().minusSeconds(1)).build();
        when(repository.findByStateHashForUpdate("expired")).thenReturn(Optional.of(state));

        assertThrows(InvalidRequestException.class, () -> store.consume("expired"));
        verify(repository).delete(state);
        verifyNoInteractions(cipher);
    }
}
