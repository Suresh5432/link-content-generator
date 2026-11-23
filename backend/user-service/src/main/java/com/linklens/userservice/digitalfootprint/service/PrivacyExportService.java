package com.linklens.userservice.digitalfootprint.service;

import com.linkcontext.contracts.EventRoutingKey;
import com.linkcontext.contracts.events.UserFootprintDeletedEvent;
import com.linklens.userservice.digitalfootprint.entity.PrivacyRequest;
import com.linklens.userservice.digitalfootprint.messaging.PrivacyEventPublisher;
import com.linklens.userservice.digitalfootprint.repo.BrowserIntegrationRepository;
import com.linklens.userservice.digitalfootprint.repo.CookieInventoryRepository;
import com.linklens.userservice.digitalfootprint.repo.PersonalDataRecordRepository;
import com.linklens.userservice.digitalfootprint.repo.PrivacyDiscoveryFindingRepository;
import com.linklens.userservice.digitalfootprint.repo.PrivacyEventOutboxRepository;
import com.linklens.userservice.digitalfootprint.repo.PrivacyRequestEvidenceRepository;
import com.linklens.userservice.digitalfootprint.repo.PrivacyRequestHistoryRepository;
import com.linklens.userservice.digitalfootprint.repo.PrivacyRequestRepository;
import com.linklens.userservice.digitalfootprint.repo.TrackedWebsiteRepository;
import com.linklens.userservice.digitalfootprint.repo.UserPrivacyPreferenceRepository;
import com.linklens.userservice.digitalfootprint.repo.UserWebsiteAccountRepository;
import com.linklens.userservice.digitalfootprint.repo.WebsitePrivacyContactRepository;
import com.linklens.userservice.digitalfootprint.repo.ConnectedEmailAccountRepository;
import com.linklens.userservice.digitalfootprint.repo.ConnectedEmailDiscoveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PrivacyExportService {

    private final TrackedWebsiteRepository websiteRepository;
    private final UserWebsiteAccountRepository accountRepository;
    private final PersonalDataRecordRepository dataRecordRepository;
    private final CookieInventoryRepository cookieRepository;
    private final WebsitePrivacyContactRepository contactRepository;
    private final UserPrivacyPreferenceRepository preferenceRepository;
    private final PrivacyRequestRepository requestRepository;
    private final PrivacyRequestEvidenceRepository evidenceRepository;
    private final PrivacyRequestHistoryRepository historyRepository;
    private final BrowserIntegrationRepository integrationRepository;
    private final PrivacyDiscoveryFindingRepository discoveryFindingRepository;
    private final PrivacyEventOutboxRepository outboxRepository;
    private final ConnectedEmailAccountRepository connectedEmailAccountRepository;
    private final ConnectedEmailDiscoveryRepository connectedEmailDiscoveryRepository;
    private final PrivacyEventPublisher eventPublisher;

    /**
     * Exports only records owned by the authenticated user. No secrets are stored,
     * so the export contains category-level privacy records and request history.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> export(String userId) {
        List<Map<String, Object>> requests = new ArrayList<>();
        for (PrivacyRequest r : requestRepository.findAllByUserIdOrderByCreatedAtDesc(userId)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("request", r);
            item.put("evidence", evidenceRepository.findAllByRequestIdAndUserIdOrderByCreatedAtAsc(r.getId(), userId));
            item.put("history", historyRepository.findAllByRequestIdAndUserIdOrderByCreatedAtAsc(r.getId(), userId));
            requests.add(item);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("exportedAt", Instant.now());
        out.put("userId", userId);
        out.put("scope", exportScope());
        out.put("retentionNotice", retentionNotice());
        out.put("trackedWebsites", websiteRepository.findAllByUserIdOrderByCreatedAtDesc(userId));
        out.put("websiteAccounts", accountRepository.findAllByUserIdOrderByCreatedAtDesc(userId));
        out.put("personalDataRecords", dataRecordRepository.findAllByUserIdOrderByCreatedAtDesc(userId));
        out.put("cookieMetadata", cookieRepository.findAllByUserIdOrderByDomainAscCookieNameAsc(userId));
        out.put("privacyContacts", contactRepository.findAllByUserIdOrderByUpdatedAtDesc(userId));
        out.put("preferences", preferenceRepository.findByUserId(userId).orElse(null));
        out.put("privacyRequests", requests);
        out.put("browserIntegrations", integrationRepository.findAllByUserIdOrderByCreatedAtDesc(userId));
        out.put("privacyDiscoveryFindings", discoveryFindingRepository.findAllByUserIdOrderByDomainAscCreatedAtAsc(userId));
        out.put("privacyEventOutbox", outboxRepository.findAllByUserIdOrderByCreatedAtDesc(userId));
        out.put("connectedEmailAccounts", connectedEmailAccountRepository.findAllByUserIdOrderByCreatedAtDesc(userId));
        out.put("connectedEmailDiscoveries", connectedEmailDiscoveryRepository.findAllByUserIdOrderByLastSeenAtDesc(userId));
        return out;
    }

    @Transactional
    public Map<String, Object> deleteAllFootprintData(String userId) {
        Map<String, Object> deleted = new LinkedHashMap<>();
        List<PrivacyRequest> requests = requestRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        deleted.put("privacyRequests", requests.size());
        requestRepository.deleteAll(requests);
        int records = dataRecordRepository.findAllByUserIdOrderByCreatedAtDesc(userId).size();
        dataRecordRepository.deleteAll(dataRecordRepository.findAllByUserIdOrderByCreatedAtDesc(userId));
        deleted.put("personalDataRecords", records);
        int accounts = accountRepository.findAllByUserIdOrderByCreatedAtDesc(userId).size();
        accountRepository.deleteAll(accountRepository.findAllByUserIdOrderByCreatedAtDesc(userId));
        deleted.put("websiteAccounts", accounts);
        int cookies = cookieRepository.findAllByUserIdOrderByDomainAscCookieNameAsc(userId).size();
        cookieRepository.deleteAll(cookieRepository.findAllByUserIdOrderByDomainAscCookieNameAsc(userId));
        deleted.put("cookieMetadata", cookies);
        int contacts = contactRepository.findAllByUserIdOrderByUpdatedAtDesc(userId).size();
        contactRepository.deleteAll(contactRepository.findAllByUserIdOrderByUpdatedAtDesc(userId));
        deleted.put("privacyContacts", contacts);
        preferenceRepository.findByUserId(userId).ifPresent(preferenceRepository::delete);
        int integrations = integrationRepository.findAllByUserIdOrderByCreatedAtDesc(userId).size();
        integrationRepository.deleteAll(integrationRepository.findAllByUserIdOrderByCreatedAtDesc(userId));
        deleted.put("browserIntegrations", integrations);
        int findings = discoveryFindingRepository.findAllByUserIdOrderByDomainAscCreatedAtAsc(userId).size();
        discoveryFindingRepository.deleteAll(discoveryFindingRepository.findAllByUserIdOrderByDomainAscCreatedAtAsc(userId));
        deleted.put("privacyDiscoveryFindings", findings);
        int outbox = outboxRepository.findAllByUserIdOrderByCreatedAtDesc(userId).size();
        outboxRepository.deleteAll(outboxRepository.findAllByUserIdOrderByCreatedAtDesc(userId));
        deleted.put("privacyEventOutbox", outbox);
        int connectedEmailDiscoveries = connectedEmailDiscoveryRepository.findAllByUserIdOrderByLastSeenAtDesc(userId).size();
        connectedEmailDiscoveryRepository.deleteAll(connectedEmailDiscoveryRepository.findAllByUserIdOrderByLastSeenAtDesc(userId));
        deleted.put("connectedEmailDiscoveries", connectedEmailDiscoveries);
        int connectedEmailAccounts = connectedEmailAccountRepository.findAllByUserIdOrderByCreatedAtDesc(userId).size();
        connectedEmailAccountRepository.deleteAll(connectedEmailAccountRepository.findAllByUserIdOrderByCreatedAtDesc(userId));
        deleted.put("connectedEmailAccounts", connectedEmailAccounts);
        int websites = websiteRepository.findAllByUserIdOrderByCreatedAtDesc(userId).size();
        websiteRepository.deleteAll(websiteRepository.findAllByUserIdOrderByCreatedAtDesc(userId));
        deleted.put("trackedWebsites", websites);

        Instant deletedAt = Instant.now();
        eventPublisher.record(EventRoutingKey.USER_FOOTPRINT_DELETED,
                new UserFootprintDeletedEvent(UUID.randomUUID(), userId, deletedAt, "FOOTPRINT_ONLY", 1));

        deleted.put("scope", exportScope());
        deleted.put("deletedAt", deletedAt);
        deleted.put("retentionNotice", retentionNotice());
        deleted.put("crossServiceDeletion", "A user.footprint.deleted.v1 event was recorded in the outbox for services that persist footprint-related copies. Other services must delete their own user-owned records through supported contracts; this endpoint does not access other service databases.");
        deleted.put("note", "Link Context footprint records were deleted. This does not delete your profile/auth account or data held by third-party websites; use your privacy requests for that.");
        return deleted;
    }

    private static Map<String, Object> exportScope() {
        Map<String, Object> scope = new LinkedHashMap<>();
        scope.put("service", "user-service");
        scope.put("dataClass", "DIGITAL_FOOTPRINT");
        scope.put("includes", List.of(
                "trackedWebsites", "websiteAccounts", "personalDataRecords", "cookieMetadata",
                "privacyContacts", "preferences", "privacyRequests", "browserIntegrations",
                "privacyDiscoveryFindings", "privacyEventOutbox"));
        scope.put("excludes", List.of(
                "user profile/account record", "authentication credentials", "third-party website/server data",
                "other service databases", "email mailbox contents", "cookie values/tokens/secrets"));
        return scope;
    }

    private static String retentionNotice() {
        return "Export/delete covers Link Context footprint records owned by this user in user-service. "
                + "It does not delete profile/auth data, third-party website data, or records in other service databases. "
                + "Other services may retain user-owned processing data until they consume the supported deletion event; "
                + "operational backups/logs, if any, follow deployment retention controls and are not removed by this API call.";
    }
}
