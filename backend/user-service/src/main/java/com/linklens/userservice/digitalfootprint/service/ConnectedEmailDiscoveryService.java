package com.linklens.userservice.digitalfootprint.service;

import com.linklens.userservice.digitalfootprint.entity.ConnectedEmailAccount;
import com.linklens.userservice.digitalfootprint.entity.ConnectedEmailDiscovery;
import com.linklens.userservice.digitalfootprint.exception.InvalidRequestException;
import com.linklens.userservice.digitalfootprint.exception.ResourceNotFoundException;
import com.linklens.userservice.digitalfootprint.repo.ConnectedEmailAccountRepository;
import com.linklens.userservice.digitalfootprint.repo.ConnectedEmailDiscoveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConnectedEmailDiscoveryService {

    private static final Set<String> ALLOWED_READONLY_SCOPES = Set.of(
            "https://www.googleapis.com/auth/gmail.metadata",
            "https://graph.microsoft.com/Mail.Read",
            "openid", "email", "profile"
    );

    private final ConnectedEmailAccountRepository accountRepository;
    private final ConnectedEmailDiscoveryRepository discoveryRepository;

    @Value("${digital-footprint.connected-email.enabled:false}")
    private boolean enabled;

    @Transactional(readOnly = true)
    public List<ConnectedEmailAccount> accounts(String userId) {
        return accountRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<ConnectedEmailDiscovery> discoveries(String userId) {
        return discoveryRepository.findAllByUserIdOrderByLastSeenAtDesc(userId);
    }

    @Transactional
    public ConnectedEmailAccount register(String userId, String provider, String accountEmail, String scopes) {
        if (!enabled) {
            throw new InvalidRequestException("Connected email discovery is disabled. Configure a provider with least-privilege read-only scopes before enabling it.");
        }
        String normalizedProvider = normalizeProvider(provider);
        String normalizedScopes = normalizeScopes(scopes);
        validateScopes(normalizedScopes);
        ConnectedEmailAccount account = ConnectedEmailAccount.builder()
                .userId(userId)
                .provider(normalizedProvider)
                .accountEmail(SensitiveDataGuard.cleanOptionalEmail(accountEmail))
                .scopes(normalizedScopes)
                .status("CONSENT_PENDING")
                .build();
        return accountRepository.save(account);
    }

    @Transactional
    public ConnectedEmailAccount recordConsent(String userId, UUID accountId) {
        ConnectedEmailAccount account = requireOwned(userId, accountId);
        if (!enabled) throw new InvalidRequestException("Connected email discovery is disabled.");
        account.setConsentedAt(Instant.now());
        account.setStatus("CONNECTED");
        return accountRepository.save(account);
    }

    @Transactional
    public void disconnect(String userId, UUID accountId) {
        ConnectedEmailAccount account = requireOwned(userId, accountId);
        discoveryRepository.deleteAllByConnectedAccountIdAndUserId(accountId, userId);
        accountRepository.delete(account);
    }

    private ConnectedEmailAccount requireOwned(String userId, UUID id) {
        return accountRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Connected email account not found"));
    }

    private static String normalizeProvider(String provider) {
        if (provider == null || provider.isBlank()) throw new InvalidRequestException("provider is required");
        return provider.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeScopes(String scopes) {
        if (scopes == null || scopes.isBlank()) throw new InvalidRequestException("scopes are required");
        return scopes.trim();
    }

    private static void validateScopes(String scopes) {
        for (String scope : scopes.split("[\\s,]+")) {
            if (scope.isBlank()) continue;
            String normalized = scope.trim();
            if (normalized.toLowerCase(Locale.ROOT).contains("mail.send")
                    || normalized.toLowerCase(Locale.ROOT).contains("mail.readwrite")
                    || normalized.toLowerCase(Locale.ROOT).contains("gmail.modify")
                    || normalized.toLowerCase(Locale.ROOT).contains("gmail.insert")
                    || normalized.toLowerCase(Locale.ROOT).contains("gmail.send")
                    || normalized.toLowerCase(Locale.ROOT).contains("mailboxsettings.readwrite")) {
                throw new InvalidRequestException("Mailbox write/delete/send scopes are not allowed");
            }
            if (!ALLOWED_READONLY_SCOPES.contains(normalized)) {
                throw new InvalidRequestException("Scope is not in the allowlist: " + normalized);
            }
        }
    }
}
