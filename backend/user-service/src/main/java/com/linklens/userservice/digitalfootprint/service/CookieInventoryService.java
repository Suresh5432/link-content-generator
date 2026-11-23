package com.linklens.userservice.digitalfootprint.service;

import com.linklens.userservice.digitalfootprint.domain.CookieCategory;
import com.linklens.userservice.digitalfootprint.dto.CookieMetadata;
import com.linklens.userservice.digitalfootprint.dto.SyncCookiesRequest;
import com.linklens.userservice.digitalfootprint.entity.BrowserIntegration;
import com.linklens.userservice.digitalfootprint.entity.BrowserSyncHistory;
import com.linklens.userservice.digitalfootprint.entity.CookieInventory;
import com.linklens.userservice.digitalfootprint.exception.InvalidRequestException;
import com.linklens.userservice.digitalfootprint.exception.ResourceNotFoundException;
import com.linklens.userservice.digitalfootprint.repo.BrowserIntegrationRepository;
import com.linklens.userservice.digitalfootprint.repo.BrowserSyncHistoryRepository;
import com.linklens.userservice.digitalfootprint.repo.CookieInventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CookieInventoryService {

    private static final int MAX_COOKIES_PER_SYNC = 5000;

    private final CookieInventoryRepository repository;
    private final BrowserIntegrationRepository integrationRepository;
    private final BrowserSyncHistoryRepository syncHistoryRepository;

    @Transactional(readOnly = true)
    public List<CookieInventory> list(String userId, String domain) {
        if (domain != null && !domain.isBlank()) {
            return repository.findAllByUserIdAndDomainOrderByCookieNameAsc(userId, domain.trim().toLowerCase(Locale.ROOT));
        }
        return repository.findAllByUserIdOrderByDomainAscCookieNameAsc(userId);
    }

    @Transactional(readOnly = true)
    public List<String> domains(String userId) {
        return repository.findDistinctDomains(userId);
    }

    @Transactional(readOnly = true)
    public List<CookieInventory> trackingCookies(String userId) {
        return repository.findAllByUserIdAndTrackingRelatedTrueOrderByDomainAsc(userId);
    }

    /**
     * Removes only the stored metadata record. The API deliberately does not and
     * cannot delete cookies from a user's browser; the extension performs that.
     */
    @CacheEvict(cacheNames = "digitalFootprintMetrics", key = "#userId")
    @Transactional
    public String deleteMetadata(String userId, UUID cookieId) {
        CookieInventory cookie = repository.findByIdAndUserId(cookieId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cookie record not found"));
        repository.delete(cookie);
        return "Cookie metadata removed from your Link Context dashboard. The cookie itself remains in your browser until you remove it with the browser extension.";
    }

    @CacheEvict(cacheNames = "digitalFootprintMetrics", key = "#userId")
    @CacheEvict(cacheNames = "digitalFootprintMetrics", key = "#userId")
    @CacheEvict(cacheNames = "digitalFootprintMetrics", key = "#userId")
    @Transactional
    public SyncResult sync(String userId, SyncCookiesRequest request) {
        if (request.cookies() == null) throw new InvalidRequestException("cookies is required");
        if (request.cookies().size() > MAX_COOKIES_PER_SYNC) {
            throw new InvalidRequestException("A single sync may contain at most " + MAX_COOKIES_PER_SYNC + " cookie metadata entries");
        }
        BrowserIntegration integration = integrationRepository.findByUserIdAndInstallationId(userId, request.installationId())
                .orElseThrow(() -> new ResourceNotFoundException("Browser integration not found; register it first"));

        int stored = 0;
        int domains = 0;
        java.util.Set<String> domainSet = new java.util.HashSet<>();
        for (CookieMetadata meta : request.cookies()) {
            if (meta == null) continue;
            String name = trimToNull(meta.name());
            String domain = normalizeDomain(meta.domain());
            if (name == null) continue;
            if (name.length() > 255) throw new InvalidRequestException("Cookie name is too long");
            CookieCategory category = CookieCategory.parse(meta.category());
            domainSet.add(domain);
            CookieInventory cookie = repository
                    .findByUserIdAndBrowserIntegrationIdAndDomainAndCookieName(userId, integration.getId(), domain, name)
                    .orElseGet(() -> CookieInventory.builder()
                            .userId(userId)
                            .browserIntegrationId(integration.getId())
                            .cookieName(name)
                            .domain(domain)
                            .firstParty(Boolean.TRUE.equals(meta.firstParty()))
                            .category(category.name())
                            .secure(Boolean.TRUE.equals(meta.secure()))
                            .httpOnly(Boolean.TRUE.equals(meta.httpOnly()))
                            .sameSite(trimToNull(meta.sameSite()))
                            .trackingRelated(Boolean.TRUE.equals(meta.trackingRelated()))
                            .source("BROWSER_EXTENSION")
                            .build());
            cookie.setExpiration(Ids.parseInstant(meta.expiration(), "expiration"));
            cookie.setPurpose(trimToNull(meta.purpose()));
            cookie.setCategory(category.name());
            cookie.setTrackingRelated(Boolean.TRUE.equals(meta.trackingRelated())
                    || category == CookieCategory.ANALYTICS
                    || category == CookieCategory.ADVERTISING
                    || category == CookieCategory.SOCIAL_MEDIA);
            repository.save(cookie);
            stored++;
        }
        domains = domainSet.size();
        integration.setLastSyncAt(Instant.now());
        integrationRepository.save(integration);
        syncHistoryRepository.save(BrowserSyncHistory.builder()
                .integrationId(integration.getId())
                .userId(userId)
                .cookieCount(stored)
                .domainsCount(domains)
                .status("SUCCESS")
                .message("Stored cookie metadata only; no cookie values were received.")
                .build());
        return new SyncResult(integration.getId(), stored, domains);
    }

    private static String normalizeDomain(String domain) {
        if (domain == null || domain.isBlank()) throw new InvalidRequestException("Cookie domain is required");
        String d = domain.trim().toLowerCase(Locale.ROOT).replaceFirst("^\\.", "").replaceFirst("^www\\.", "");
        if (d.length() > 255) throw new InvalidRequestException("Cookie domain is too long");
        return d;
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record SyncResult(UUID integrationId, int cookiesStored, int domains) {}
}
