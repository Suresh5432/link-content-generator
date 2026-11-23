package com.linklens.userservice.digitalfootprint.service;

import com.linklens.userservice.digitalfootprint.entity.ConnectedEmailAccount;
import com.linklens.userservice.digitalfootprint.entity.ConnectedEmailDiscovery;
import com.linklens.userservice.digitalfootprint.exception.InvalidRequestException;
import com.linklens.userservice.digitalfootprint.exception.ResourceNotFoundException;
import com.linklens.userservice.digitalfootprint.repo.ConnectedEmailAccountRepository;
import com.linklens.userservice.digitalfootprint.repo.ConnectedEmailDiscoveryRepository;
import com.linklens.userservice.digitalfootprint.service.ConnectedEmailTokenCipher.EncryptedValue;
import com.linklens.userservice.digitalfootprint.service.ConnectedEmailOAuthStateStore.ConsumedState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConnectedEmailOAuthServiceTest {
    @Mock ConnectedEmailAccountRepository accountRepository;
    @Mock ConnectedEmailDiscoveryRepository discoveryRepository;
    @Mock ConnectedEmailOAuthStateStore stateStore;
    @Mock ConnectedEmailTokenCipher tokenCipher;
    @Mock GoogleEmailProviderClient googleClient;
    @Mock GoogleEmailProvider googleProvider;
    ConnectedEmailOAuthService service;

    @BeforeEach
    void setUp() {
        service = new ConnectedEmailOAuthService(accountRepository, discoveryRepository, stateStore,
                tokenCipher, googleClient, googleProvider);
        ReflectionTestUtils.setField(service, "enabled", true);
        lenient().when(googleClient.isConfigured()).thenReturn(true);
        lenient().when(tokenCipher.isConfigured()).thenReturn(true);
    }

    @Test
    void requiresExplicitConsentAndPersistsOnlyHashedOAuthState() {
        assertThrows(InvalidRequestException.class, () -> service.beginAuthorization("user-1", false));
        verifyNoInteractions(stateStore);

        when(googleClient.authorizationUrl(anyString(), anyString())).thenReturn("https://accounts.google.test/oauth");
        assertEquals("https://accounts.google.test/oauth", service.beginAuthorization("user-1", true));

        ArgumentCaptor<String> stateHash = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> verifier = ArgumentCaptor.forClass(String.class);
        verify(stateStore).save(stateHash.capture(), eq("user-1"), verifier.capture(), any(Instant.class));
        assertEquals(64, stateHash.getValue().length());
        assertNotEquals(stateHash.getValue(), verifier.getValue());
        verify(googleClient).authorizationUrl(anyString(), anyString());
    }

    @Test
    void rejectsConnectionWhenProviderOrEncryptionConfigurationIsMissing() {
        when(tokenCipher.isConfigured()).thenReturn(false);
        assertFalse(service.status().configured());
        assertThrows(GoogleEmailProviderException.class, () -> service.beginAuthorization("user-1", true));
        verifyNoInteractions(stateStore);
    }

    @Test
    void completesOAuthAndStoresOnlyEncryptedRefreshToken() {
        when(stateStore.consume(anyString())).thenReturn(new ConsumedState("user-1", "pkce-verifier"));
        when(googleProvider.exchangeCode("authorization-code", "pkce-verifier"))
                .thenReturn(new GoogleEmailProvider.OAuthTokens(
                        "temporary-access-token", "synthetic-refresh-token",
                        "openid email https://www.googleapis.com/auth/gmail.metadata"));
        when(googleProvider.mailboxEmail("temporary-access-token")).thenReturn("person@example.test");
        when(tokenCipher.encrypt("synthetic-refresh-token")).thenReturn(new EncryptedValue(new byte[]{1, 2, 3}, new byte[]{4, 5}));
        when(accountRepository.findByUserIdAndProvider("user-1", "GOOGLE")).thenReturn(Optional.empty());

        service.completeAuthorization("oauth-state", "authorization-code");

        ArgumentCaptor<ConnectedEmailAccount> saved = ArgumentCaptor.forClass(ConnectedEmailAccount.class);
        verify(accountRepository).save(saved.capture());
        assertEquals("person@example.test", saved.getValue().getAccountEmail());
        assertEquals("CONNECTED", saved.getValue().getStatus());
        assertArrayEquals(new byte[]{1, 2, 3}, saved.getValue().getRefreshTokenCiphertext());
        assertArrayEquals(new byte[]{4, 5}, saved.getValue().getRefreshTokenIv());
        assertTrue(saved.getValue().getScopes().contains("gmail.metadata"));
    }

    @Test
    void rejectsUnexpectedOAuthScopesAndNeverStoresTheToken() {
        when(stateStore.consume(anyString())).thenReturn(new ConsumedState("user-1", "pkce-verifier"));
        when(googleProvider.exchangeCode("authorization-code", "pkce-verifier"))
                .thenReturn(new GoogleEmailProvider.OAuthTokens(
                        "temporary-access-token", "synthetic-refresh-token",
                        "https://mail.google.com/"));

        assertThrows(GoogleEmailProviderException.class,
                () -> service.completeAuthorization("oauth-state", "authorization-code"));

        verifyNoInteractions(tokenCipher);
        verifyNoInteractions(accountRepository);
    }

    @Test
    void scanStoresOnlyLowConfidenceHeaderEvidenceAndNoMessageContent() {
        UUID accountId = UUID.randomUUID();
        ConnectedEmailAccount account = ConnectedEmailAccount.builder().id(accountId).userId("user-1")
                .provider("GOOGLE").status("CONNECTED").refreshTokenCiphertext(new byte[]{1})
                .refreshTokenIv(new byte[]{2}).build();
        when(accountRepository.findByIdAndUserId(accountId, "user-1")).thenReturn(Optional.of(account));
        when(tokenCipher.decrypt(new byte[]{1}, new byte[]{2})).thenReturn("synthetic-refresh-token");
        when(googleProvider.refreshAccessToken("synthetic-refresh-token"))
                .thenReturn(new GoogleEmailProvider.OAuthTokens("temporary-access-token", null, ""));
        Instant receivedAt = Instant.parse("2026-01-02T03:04:05Z");
        when(googleProvider.recentRegistrationMessages("temporary-access-token"))
                .thenReturn(new GoogleEmailProvider.MailboxScanResult(12, List.of(
                        new GoogleEmailProvider.MailboxMessageMetadata("shop.example", "verification", receivedAt))));
        when(discoveryRepository.findFirstByConnectedAccountIdAndDomainAndEvidenceType(accountId, "shop.example", "REGISTRATION_EMAIL"))
                .thenReturn(Optional.empty());
        when(discoveryRepository.save(any())).thenAnswer(invocation -> {
            ConnectedEmailDiscovery discovery = invocation.getArgument(0);
            discovery.setId(UUID.randomUUID());
            return discovery;
        });

        var result = service.scan("user-1", accountId);

        assertEquals(12, result.messagesReviewed());
        assertEquals(1, result.possibleAccountsFound());
        assertFalse(result.discoveries().getFirst().confirmedByUser());
        assertEquals("LOW", result.discoveries().getFirst().confidence());
        assertTrue(result.discoveries().getFirst().evidenceSummary().contains("verification"));
        assertFalse(result.discoveries().getFirst().evidenceSummary().contains("message body"));
        verify(accountRepository).save(account);
    }

    @Test
    void rejectsCrossUserScans() {
        UUID accountId = UUID.randomUUID();
        when(accountRepository.findByIdAndUserId(accountId, "other-user")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.scan("other-user", accountId));

        verifyNoInteractions(googleProvider);
    }

    @Test
    void doesNotAllowAnotherUserToConfirmOrDismissDiscovery() {
        UUID discoveryId = UUID.randomUUID();
        when(discoveryRepository.findByIdAndUserId(discoveryId, "other-user")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.confirm("other-user", discoveryId));
        assertThrows(ResourceNotFoundException.class, () -> service.dismiss("other-user", discoveryId));
        verify(discoveryRepository, never()).save(any());
    }

    @Test
    void confirmsAndDismissesOnlyOwnedDiscoveriesAndDeletesImportedRecords() {
        UUID discoveryId = UUID.randomUUID();
        ConnectedEmailDiscovery discovery = ConnectedEmailDiscovery.builder()
                .id(discoveryId).userId("user-1").confirmedByUser(false).dismissedByUser(false).build();
        when(discoveryRepository.findByIdAndUserId(discoveryId, "user-1")).thenReturn(Optional.of(discovery));
        when(discoveryRepository.save(discovery)).thenReturn(discovery);

        assertTrue(service.confirm("user-1", discoveryId).confirmedByUser());
        assertFalse(discovery.isDismissedByUser());
        assertTrue(service.dismiss("user-1", discoveryId).dismissedByUser());
        service.deleteDiscovery("user-1", discoveryId);

        verify(discoveryRepository, times(2)).save(discovery);
        verify(discoveryRepository).delete(discovery);
    }

    @Test
    void deletesLocalCredentialsEvenIfProviderRevocationCannotBeConfirmed() {
        UUID accountId = UUID.randomUUID();
        ConnectedEmailAccount account = ConnectedEmailAccount.builder().id(accountId).userId("user-1")
                .provider("GOOGLE").status("CONNECTED").refreshTokenCiphertext(new byte[]{1})
                .refreshTokenIv(new byte[]{2}).build();
        when(accountRepository.findByIdAndUserId(accountId, "user-1")).thenReturn(Optional.of(account));
        when(tokenCipher.decrypt(new byte[]{1}, new byte[]{2})).thenReturn("synthetic-refresh-token");
        when(googleProvider.revoke("synthetic-refresh-token")).thenReturn(false);

        var result = service.disconnect("user-1", accountId);

        assertFalse(result.providerRevocationConfirmed());
        assertTrue(result.message().contains("revoke Link Context access"));
        verify(discoveryRepository).deleteAllByConnectedAccountIdAndUserId(accountId, "user-1");
        verify(accountRepository).delete(account);
    }
}
