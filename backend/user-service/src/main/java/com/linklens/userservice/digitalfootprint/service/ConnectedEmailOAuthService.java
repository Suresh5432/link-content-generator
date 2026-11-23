package com.linklens.userservice.digitalfootprint.service;

import com.linklens.userservice.digitalfootprint.dto.ConnectedEmailAccountResponse;
import com.linklens.userservice.digitalfootprint.dto.ConnectedEmailDiscoveryResponse;
import com.linklens.userservice.digitalfootprint.dto.ConnectedEmailScanResponse;
import com.linklens.userservice.digitalfootprint.dto.ConnectedEmailStatusResponse;
import com.linklens.userservice.digitalfootprint.entity.ConnectedEmailAccount;
import com.linklens.userservice.digitalfootprint.entity.ConnectedEmailDiscovery;
import com.linklens.userservice.digitalfootprint.exception.InvalidRequestException;
import com.linklens.userservice.digitalfootprint.exception.ResourceNotFoundException;
import com.linklens.userservice.digitalfootprint.repo.ConnectedEmailAccountRepository;
import com.linklens.userservice.digitalfootprint.repo.ConnectedEmailDiscoveryRepository;
import com.linklens.userservice.digitalfootprint.service.ConnectedEmailOAuthStateStore.ConsumedState;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConnectedEmailOAuthService {
    private static final String PROVIDER = "GOOGLE";
    private static final String REQUIRED_SCOPE = "https://www.googleapis.com/auth/gmail.metadata";
    private static final Set<String> ALLOWED_SCOPES = Set.of(
            "openid", "email", "profile", "https://www.googleapis.com/auth/userinfo.email", REQUIRED_SCOPE
    );
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ConnectedEmailAccountRepository accountRepository;
    private final ConnectedEmailDiscoveryRepository discoveryRepository;
    private final ConnectedEmailOAuthStateStore stateStore;
    private final ConnectedEmailTokenCipher tokenCipher;
    private final GoogleEmailProviderClient googleProvider;
    private final GoogleEmailProvider googleEmailProvider;

    @Value("${digital-footprint.connected-email.enabled:false}")
    private boolean enabled;

    @Transactional(readOnly = true)
    public List<ConnectedEmailAccountResponse> accounts(String userId) {
        return accountRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(ConnectedEmailAccountResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ConnectedEmailDiscoveryResponse> discoveries(String userId) {
        return discoveryRepository.findAllByUserIdAndDismissedByUserFalseOrderByLastSeenAtDesc(userId).stream()
                .map(ConnectedEmailDiscoveryResponse::from).toList();
    }

    public ConnectedEmailStatusResponse status() {
        boolean configured = enabled && googleProvider.isConfigured() && tokenCipher.isConfigured();
        return new ConnectedEmailStatusResponse(configured, PROVIDER, REQUIRED_SCOPE,
                configured
                        ? "Checks only the latest 100 message headers for registration-related subjects. Email bodies are never requested or stored; discoveries are possible matches, not proof of an account."
                        : "Gmail discovery is not configured in this deployment. Provider OAuth credentials, callback URL, and a 32-byte Base64 encryption key are required.");
    }

    public String beginAuthorization(String userId, boolean consent) {
        requireConfigured();
        if (!consent) throw new InvalidRequestException("Explicit consent is required before connecting Gmail.");
        String state = randomUrlSafe(32);
        String verifier = randomUrlSafe(64);
        stateStore.save(sha256(state), userId, verifier, Instant.now().plusSeconds(300));
        return googleProvider.authorizationUrl(state, sha256Base64(verifier));
    }

    public void rejectAuthorization(String state) {
        stateStore.consume(sha256(requireText(state, "OAuth state is required")));
    }

    public void completeAuthorization(String state, String code) {
        requireConfigured();
        ConsumedState consumed = stateStore.consume(sha256(requireText(state, "OAuth state is required")));
        GoogleEmailProvider.OAuthTokens tokens = googleEmailProvider.exchangeCode(
                requireText(code, "Authorization code is required"), consumed.verifier());
        validateGrantedScopes(tokens.grantedScopes());
        if (tokens.refreshToken() == null || tokens.refreshToken().isBlank()) {
            throw new GoogleEmailProviderException("Google did not return a refresh token. Revoke the app's access and connect again.");
        }
        String email = googleEmailProvider.mailboxEmail(tokens.accessToken());
        ConnectedEmailTokenCipher.EncryptedValue encryptedToken = tokenCipher.encrypt(tokens.refreshToken());
        ConnectedEmailAccount account = accountRepository.findByUserIdAndProvider(consumed.userId(), PROVIDER)
                .orElseGet(() -> ConnectedEmailAccount.builder().userId(consumed.userId()).provider(PROVIDER).build());
        account.setAccountEmail(email);
        account.setScopes(String.join(" ", allowedGrantedScopes(tokens.grantedScopes())));
        account.setStatus("CONNECTED");
        account.setConsentedAt(Instant.now());
        account.setRefreshTokenCiphertext(encryptedToken.ciphertext());
        account.setRefreshTokenIv(encryptedToken.iv());
        accountRepository.save(account);
    }

    public ConnectedEmailScanResponse scan(String userId, UUID accountId) {
        ConnectedEmailAccount account = requireConnectedAccount(userId, accountId);
        String refreshToken = tokenCipher.decrypt(account.getRefreshTokenCiphertext(), account.getRefreshTokenIv());
        GoogleEmailProvider.OAuthTokens access = googleEmailProvider.refreshAccessToken(refreshToken);
        GoogleEmailProvider.MailboxScanResult scan =
                googleEmailProvider.recentRegistrationMessages(access.accessToken());
        List<GoogleEmailProvider.MailboxMessageMetadata> messages = scan.matches();
        Map<String, ConnectedEmailDiscovery> found = new LinkedHashMap<>();
        Instant now = Instant.now();
        for (GoogleEmailProvider.MailboxMessageMetadata message : messages) {
            String domain = normalizeDomain(message.senderDomain());
            if (domain == null) continue;
            ConnectedEmailDiscovery discovery = discoveryRepository
                    .findFirstByConnectedAccountIdAndDomainAndEvidenceType(accountId, domain, "REGISTRATION_EMAIL")
                    .orElseGet(() -> ConnectedEmailDiscovery.builder()
                            .connectedAccountId(accountId)
                            .userId(userId)
                            .provider(PROVIDER)
                            .domain(domain)
                            .evidenceType("REGISTRATION_EMAIL")
                            .firstSeenAt(message.receivedAt() == null ? now : message.receivedAt())
                            .confidence("LOW")
                            .confirmedByUser(false)
                            .dismissedByUser(false)
                            .build());
            Instant received = message.receivedAt() == null ? now : message.receivedAt();
            if (discovery.getFirstSeenAt() == null || received.isBefore(discovery.getFirstSeenAt())) {
                discovery.setFirstSeenAt(received);
            }
            if (discovery.getLastSeenAt() == null || received.isAfter(discovery.getLastSeenAt())) {
                discovery.setLastSeenAt(received);
            }
            discovery.setEvidenceSource("Gmail header matched a registration-related subject category ("
                    + safeCategory(message.subjectCategory()) + "); sender domain " + domain
                    + ". Message body and full subject were not accessed or stored.");
            found.put(domain, discoveryRepository.save(discovery));
        }
        account.setLastScanAt(now);
        accountRepository.save(account);
        List<ConnectedEmailDiscoveryResponse> response = new ArrayList<>();
        found.values().forEach(item -> response.add(ConnectedEmailDiscoveryResponse.from(item)));
        return new ConnectedEmailScanResponse(scan.messagesReviewed(), response.size(), response);
    }

    @Transactional
    public ConnectedEmailDiscoveryResponse confirm(String userId, UUID discoveryId) {
        ConnectedEmailDiscovery discovery = requireOwnedDiscovery(userId, discoveryId);
        discovery.setConfirmedByUser(true);
        discovery.setDismissedByUser(false);
        return ConnectedEmailDiscoveryResponse.from(discoveryRepository.save(discovery));
    }

    @Transactional
    public ConnectedEmailDiscoveryResponse dismiss(String userId, UUID discoveryId) {
        ConnectedEmailDiscovery discovery = requireOwnedDiscovery(userId, discoveryId);
        discovery.setDismissedByUser(true);
        return ConnectedEmailDiscoveryResponse.from(discoveryRepository.save(discovery));
    }

    @Transactional
    public void deleteDiscovery(String userId, UUID discoveryId) {
        discoveryRepository.delete(requireOwnedDiscovery(userId, discoveryId));
    }

    public DisconnectResult disconnect(String userId, UUID accountId) {
        ConnectedEmailAccount account = requireConnectedAccount(userId, accountId);
        boolean revoked = false;
        if (account.getRefreshTokenCiphertext() != null && account.getRefreshTokenIv() != null) {
            try {
                String refreshToken = tokenCipher.decrypt(account.getRefreshTokenCiphertext(), account.getRefreshTokenIv());
                revoked = googleEmailProvider.revoke(refreshToken);
            } catch (GoogleEmailProviderException | IllegalStateException exception) {
                revoked = false;
            }
        }
        discoveryRepository.deleteAllByConnectedAccountIdAndUserId(accountId, userId);
        accountRepository.delete(account);
        return new DisconnectResult(revoked,
                revoked ? "Gmail disconnected and provider access revoked."
                        : "Gmail was disconnected and local credentials deleted. Provider revocation could not be confirmed; revoke Link Context access in your Google Account settings.");
    }

    private void requireConfigured() {
        if (!status().configured()) throw new GoogleEmailProviderException(status().limitation());
    }

    private ConnectedEmailAccount requireConnectedAccount(String userId, UUID accountId) {
        ConnectedEmailAccount account = accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Connected email account not found"));
        if (!"CONNECTED".equals(account.getStatus()) || account.getRefreshTokenCiphertext() == null
                || account.getRefreshTokenIv() == null) {
            throw new InvalidRequestException("Connect Gmail before scanning.");
        }
        return account;
    }

    private ConnectedEmailDiscovery requireOwnedDiscovery(String userId, UUID discoveryId) {
        return discoveryRepository.findByIdAndUserId(discoveryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Email discovery not found"));
    }

    private static void validateGrantedScopes(String scopes) {
        Set<String> granted = allowedGrantedScopes(scopes);
        if (!granted.contains(REQUIRED_SCOPE)) {
            throw new GoogleEmailProviderException("Gmail did not grant the required read-only metadata scope.");
        }
    }

    private static Set<String> allowedGrantedScopes(String scopes) {
        if (scopes == null || scopes.isBlank()) return Set.of();
        Set<String> granted = new java.util.HashSet<>();
        for (String scope : scopes.trim().split("\\s+")) {
            if (!ALLOWED_SCOPES.contains(scope)) {
                throw new GoogleEmailProviderException("Google granted a scope that is not allowed. Disconnect the integration and review the consent screen.");
            }
            granted.add(scope);
        }
        return granted;
    }

    private static String normalizeDomain(String domain) {
        if (domain == null) return null;
        String normalized = domain.trim().toLowerCase(Locale.ROOT).replaceFirst("^www\\.", "");
        return normalized.matches("(?=.{1,253}$)([a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,63}") ? normalized : null;
    }

    private static String safeCategory(String category) {
        if (category == null) return "account";
        return switch (category.toLowerCase(Locale.ROOT)) {
            case "welcome", "verification", "registration", "account" -> category.toLowerCase(Locale.ROOT);
            default -> "account";
        };
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new InvalidRequestException(name);
        return value;
    }

    private static String randomUrlSafe(int bytes) {
        byte[] value = new byte[bytes];
        SECURE_RANDOM.nextBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private static String sha256(String value) {
        return HexFormat.of().formatHex(digest(value));
    }

    private static String sha256Base64(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest(value));
    }

    private static byte[] digest(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public record DisconnectResult(boolean providerRevocationConfirmed, String message) {}
}
