package com.linklens.userservice.digitalfootprint.service;

import com.linklens.userservice.digitalfootprint.dto.FootprintContextResponse;
import com.linklens.userservice.digitalfootprint.entity.PersonalDataRecord;
import com.linklens.userservice.digitalfootprint.entity.PrivacyRequest;
import com.linklens.userservice.digitalfootprint.entity.TrackedWebsite;
import com.linklens.userservice.digitalfootprint.repo.CookieInventoryRepository;
import com.linklens.userservice.digitalfootprint.repo.PersonalDataRecordRepository;
import com.linklens.userservice.digitalfootprint.repo.PrivacyRequestRepository;
import com.linklens.userservice.digitalfootprint.repo.TrackedWebsiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Builds a bounded, category-level snapshot for the grounded privacy assistant.
 * Only rows owned by the supplied user id are ever read.
 */
@Service
@RequiredArgsConstructor
public class FootprintContextService {

    private static final int MAX_WEBSITES = 100;
    private static final int MAX_DATA_RECORDS = 500;
    private static final int MAX_REQUESTS = 200;

    private final TrackedWebsiteRepository websiteRepository;
    private final PersonalDataRecordRepository dataRecordRepository;
    private final PrivacyRequestRepository requestRepository;
    private final CookieInventoryRepository cookieRepository;

    @Transactional(readOnly = true)
    public FootprintContextResponse buildContext(String userId) {
        List<TrackedWebsite> websites = websiteRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        Map<UUID, String> names = new HashMap<>();
        for (TrackedWebsite website : websites) {
            names.put(website.getId(), website.getName());
        }

        List<FootprintContextResponse.Website> websiteSummaries = websites.stream()
                .limit(MAX_WEBSITES)
                .map(w -> new FootprintContextResponse.Website(
                        w.getName(), w.getDomain(), w.getCategory(), w.getRiskLevel(),
                        w.getAccountStatus(), w.getDataCategories(), w.getTrackingTechnologies(),
                        w.getDeletionRequestStatus(), w.getPrivacyPolicyUrl(), w.getDeletionPolicyUrl()))
                .toList();

        List<FootprintContextResponse.DataRecord> dataSummaries = dataRecordRepository
                .findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .limit(MAX_DATA_RECORDS)
                .map(r -> new FootprintContextResponse.DataRecord(
                        names.getOrDefault(r.getTrackedWebsiteId(), "unknown website"),
                        r.getDataCategory(), r.getDataType(), r.getDescription(),
                        sourceLabel(r), r.getEvidenceSource(),
                        r.getDateShared() == null ? null : r.getDateShared().toString()))
                .toList();

        List<FootprintContextResponse.RequestRecord> requestSummaries = requestRepository
                .findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .limit(MAX_REQUESTS)
                .map(r -> new FootprintContextResponse.RequestRecord(
                        names.getOrDefault(r.getTrackedWebsiteId(), "unknown website"),
                        r.getRequestType(), r.getStatus(),
                        r.getSubmittedAt() == null ? null : r.getSubmittedAt().toString(),
                        r.getExpectedResponseDate() == null ? null : r.getExpectedResponseDate().toString(),
                        r.isWebsiteClaimedDeletion(), r.isUserVerified()))
                .toList();

        List<String> cookieDomains = cookieRepository.findDistinctDomains(userId);
        long trackingCookies = cookieRepository.countByUserIdAndTrackingRelatedTrue(userId);

        return new FootprintContextResponse(websiteSummaries, dataSummaries, requestSummaries,
                cookieDomains, trackingCookies);
    }

    private static String sourceLabel(PersonalDataRecord record) {
        if (record.isConfirmedByUser()) return "USER_CONFIRMED";
        if (record.isObservedOnWebsite()) return "OBSERVED_ON_WEBSITE";
        if (record.isDeclaredInPolicy()) return "DECLARED_IN_POLICY";
        if (record.isInferredByAi()) return "INFERRED_BY_AI";
        return record.getSource() == null ? "UNKNOWN" : record.getSource();
    }
}
