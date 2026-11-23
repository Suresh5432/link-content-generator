package com.linklens.userservice.digitalfootprint.service;

import com.linkcontext.contracts.events.PrivacyDiscoveryDiscoveredEvent;
import com.linklens.userservice.digitalfootprint.dto.UpdatePrivacyContactRequest;
import com.linklens.userservice.digitalfootprint.entity.PrivacyDiscoveryFinding;
import com.linklens.userservice.digitalfootprint.entity.TrackedWebsite;
import com.linklens.userservice.digitalfootprint.repo.PrivacyDiscoveryFindingRepository;
import com.linklens.userservice.digitalfootprint.repo.TrackedWebsiteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores publicly discovered privacy resources as evidence and applies them to
 * a tracked website only when that website belongs to the same user. All
 * findings remain unverified hints for the user to review.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PrivacyDiscoveryService {

    private static final int MAX_FINDINGS = 50;
    private static final int MAX_SOURCE_URLS_LENGTH = 4000;

    private final PrivacyDiscoveryFindingRepository findingRepository;
    private final TrackedWebsiteRepository websiteRepository;
    private final WebsitePrivacyContactService contactService;

    @Transactional
    public void ingest(PrivacyDiscoveryDiscoveredEvent event) {
        if (event == null || event.version() != 1) {
            log.warn("Ignoring unsupported privacy discovery event version");
            return;
        }
        if (event.userId() == null || event.userId().isBlank()
                || event.domain() == null || event.domain().isBlank()) {
            log.warn("Ignoring privacy discovery event without user or domain");
            return;
        }
        String domain = normalizeDomain(event.domain());
        List<PrivacyDiscoveryDiscoveredEvent.Finding> findings = event.findings() == null
                ? List.of() : event.findings().stream().limit(MAX_FINDINGS).toList();

        for (PrivacyDiscoveryDiscoveredEvent.Finding finding : findings) {
            if (finding == null || finding.type() == null || finding.type().isBlank()) continue;
            String sourceUrl = trimToNull(finding.sourceUrl());
            if (findingRepository.existsByUserIdAndDomainAndFindingTypeAndSourceUrl(
                    event.userId(), domain, finding.type().trim(), sourceUrl)) continue;
            findingRepository.save(PrivacyDiscoveryFinding.builder()
                    .userId(event.userId())
                    .domain(domain)
                    .findingType(trimTo(finding.type(), 60))
                    .name(trimTo(finding.name(), 255))
                    .sourceUrl(sourceUrl == null ? null : trimTo(sourceUrl, 4000))
                    .description(trimTo(finding.description(), 2000))
                    .confidence(trimTo(finding.confidence(), 20))
                    .analysisId(event.analysisId())
                    .build());
        }
        applyToWebsite(event.userId(), domain);
    }

    @Transactional
    public void applyToWebsite(String userId, String domain) {
        if (userId == null || domain == null || domain.isBlank()) return;
        TrackedWebsite website = websiteRepository.findFirstByUserIdAndDomain(userId, normalizeDomain(domain)).orElse(null);
        if (website == null) return;

        List<PrivacyDiscoveryFinding> findings = findingRepository
                .findAllByUserIdAndDomainOrderByCreatedAtAsc(userId, normalizeDomain(domain));
        if (findings.isEmpty()) return;

        List<String> sources = new ArrayList<>();
        String privacyPolicy = null;
        String deletionPolicy = null;
        String accessRequest = null;
        String accountDeletion = null;
        String cookieManagement = null;
        String consentManagement = null;
        String email = null;
        String dpo = null;

        for (PrivacyDiscoveryFinding finding : findings) {
            String source = finding.getSourceUrl();
            if (source != null && !sources.contains(source)) sources.add(source);
            String value = source == null ? finding.getName() : source;
            switch (finding.getFindingType()) {
                case "PRIVACY_POLICY" -> privacyPolicy = value;
                case "DATA_DELETION_PAGE" -> deletionPolicy = value;
                case "DATA_ACCESS_PAGE" -> accessRequest = value;
                case "ACCOUNT_DELETION_PAGE" -> accountDeletion = value;
                case "COOKIE_MANAGEMENT_PAGE" -> cookieManagement = value;
                case "CONSENT_MANAGEMENT_PAGE" -> consentManagement = value;
                case "PRIVACY_CONTACT_EMAIL" -> email = finding.getName();
                case "DPO_CONTACT" -> dpo = value;
                default -> { }
            }
        }

        String joinedSources = String.join("\n", sources);
        if (joinedSources.length() > MAX_SOURCE_URLS_LENGTH) {
            joinedSources = joinedSources.substring(0, MAX_SOURCE_URLS_LENGTH);
        }
        contactService.upsert(userId, website.getId(), new UpdatePrivacyContactRequest(
                privacyPolicy, deletionPolicy, accessRequest, accountDeletion,
                cookieManagement, consentManagement, email, dpo, joinedSources));

        if (website.getPrivacyPolicyUrl() == null && privacyPolicy != null) {
            website.setPrivacyPolicyUrl(privacyPolicy);
        }
        if (website.getDeletionPolicyUrl() == null && deletionPolicy != null) {
            website.setDeletionPolicyUrl(deletionPolicy);
        }
        websiteRepository.save(website);
        log.info("Applied {} privacy discovery findings to website {} for user {}",
                findings.size(), website.getId(), userId);
    }

    private static String normalizeDomain(String domain) {
        return domain.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static String trimToNull(String value) {
        String trimmed = value == null ? null : value.trim();
        return trimmed == null || trimmed.isEmpty() ? null : trimmed;
    }

    private static String trimTo(String value, int max) {
        String trimmed = trimToNull(value);
        if (trimmed == null) return null;
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}
