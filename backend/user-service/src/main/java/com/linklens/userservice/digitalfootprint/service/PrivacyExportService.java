package com.linklens.userservice.digitalfootprint.service;

import com.linklens.userservice.digitalfootprint.entity.PrivacyRequest;
import com.linklens.userservice.digitalfootprint.repo.BrowserIntegrationRepository;
import com.linklens.userservice.digitalfootprint.repo.CookieInventoryRepository;
import com.linklens.userservice.digitalfootprint.repo.PersonalDataRecordRepository;
import com.linklens.userservice.digitalfootprint.repo.PrivacyRequestEvidenceRepository;
import com.linklens.userservice.digitalfootprint.repo.PrivacyRequestHistoryRepository;
import com.linklens.userservice.digitalfootprint.repo.PrivacyRequestRepository;
import com.linklens.userservice.digitalfootprint.repo.TrackedWebsiteRepository;
import com.linklens.userservice.digitalfootprint.repo.UserPrivacyPreferenceRepository;
import com.linklens.userservice.digitalfootprint.repo.UserWebsiteAccountRepository;
import com.linklens.userservice.digitalfootprint.repo.WebsitePrivacyContactRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        out.put("retentionNotice", "This export contains records you created in Link Context. It does not prove what a website stores on its servers.");
        out.put("trackedWebsites", websiteRepository.findAllByUserIdOrderByCreatedAtDesc(userId));
        out.put("websiteAccounts", accountRepository.findAllByUserIdOrderByCreatedAtDesc(userId));
        out.put("personalDataRecords", dataRecordRepository.findAllByUserIdOrderByCreatedAtDesc(userId));
        out.put("cookieMetadata", cookieRepository.findAllByUserIdOrderByDomainAscCookieNameAsc(userId));
        out.put("privacyContacts", contactRepository.findAllByUserIdOrderByUpdatedAtDesc(userId));
        out.put("preferences", preferenceRepository.findByUserId(userId).orElse(null));
        out.put("privacyRequests", requests);
        out.put("browserIntegrations", integrationRepository.findAllByUserIdOrderByCreatedAtDesc(userId));
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
        int websites = websiteRepository.findAllByUserIdOrderByCreatedAtDesc(userId).size();
        websiteRepository.deleteAll(websiteRepository.findAllByUserIdOrderByCreatedAtDesc(userId));
        deleted.put("trackedWebsites", websites);
        deleted.put("note", "Link Context footprint records were deleted. This does not delete data held by third-party websites; use your privacy requests for that.");
        return deleted;
    }
}
