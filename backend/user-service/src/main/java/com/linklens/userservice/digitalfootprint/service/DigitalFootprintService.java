package com.linklens.userservice.digitalfootprint.service;

import com.linklens.userservice.digitalfootprint.domain.PrivacyRequestStatus;
import com.linklens.userservice.digitalfootprint.dto.CreateTrackedWebsiteRequest;
import com.linklens.userservice.digitalfootprint.dto.ImportWebsiteRequest;
import com.linklens.userservice.digitalfootprint.dto.UpdateTrackedWebsiteRequest;
import com.linklens.userservice.digitalfootprint.entity.CookieInventory;
import com.linklens.userservice.digitalfootprint.entity.PersonalDataRecord;
import com.linklens.userservice.digitalfootprint.entity.PrivacyRequest;
import com.linklens.userservice.digitalfootprint.entity.TrackedWebsite;
import com.linklens.userservice.digitalfootprint.exception.InvalidRequestException;
import com.linklens.userservice.digitalfootprint.exception.ResourceNotFoundException;
import com.linklens.userservice.digitalfootprint.repo.CookieInventoryRepository;
import com.linklens.userservice.digitalfootprint.repo.PersonalDataRecordRepository;
import com.linklens.userservice.digitalfootprint.repo.PrivacyRequestRepository;
import com.linklens.userservice.digitalfootprint.repo.TrackedWebsiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DigitalFootprintService {

    private static final List<String> PENDING_STATUSES = List.of(
            PrivacyRequestStatus.SUBMITTED.name(),
            PrivacyRequestStatus.ACKNOWLEDGED.name(),
            PrivacyRequestStatus.IN_PROGRESS.name(),
            PrivacyRequestStatus.NEEDS_VERIFICATION.name());

    private static final List<String> SENSITIVE_CATEGORIES = List.of("FINANCIAL", "IDENTITY", "HEALTH", "GOVERNMENT");

    private final TrackedWebsiteRepository repository;
    private final PersonalDataRecordRepository dataRecordRepository;
    private final PrivacyRequestRepository privacyRequestRepository;
    private final CookieInventoryRepository cookieRepository;
    private final PrivacyDiscoveryService privacyDiscoveryService;

    @CacheEvict(cacheNames = "digitalFootprintMetrics", key = "#userId")
    @Transactional
    public TrackedWebsite create(String userId, CreateTrackedWebsiteRequest request) {
        TrackedWebsite w = TrackedWebsite.builder()
                .userId(userId)
                .name(request.name().trim())
                .domain(normalizeDomain(request.domain()))
                .url(request.url().trim())
                .category(trimToNull(request.category()))
                .accountStatus(trimToNull(request.accountStatus()))
                .privacyPolicyUrl(trimToNull(request.privacyPolicyUrl()))
                .deletionPolicyUrl(trimToNull(request.deletionPolicyUrl()))
                .riskLevel(trimToNull(request.riskLevel()))
                .dataCategories(trimToNull(request.dataCategories()))
                .trackingTechnologies(trimToNull(request.trackingTechnologies()))
                .fromAnalysisId(Ids.parseNullableUuid(request.fromAnalysisId(), "fromAnalysisId"))
                .firstRecordedAt(Instant.now())
                .build();
        TrackedWebsite saved = repository.save(w);
        privacyDiscoveryService.applyToWebsite(userId, saved.getDomain());
        return saved;
    }

    @CacheEvict(cacheNames = "digitalFootprintMetrics", key = "#userId")
    @Transactional
    public TrackedWebsite importFromAnalysis(String userId, ImportWebsiteRequest request) {
        UUID analysisId = Ids.parseUuid(request.analysisId(), "analysisId");
        Optional<TrackedWebsite> existing = repository.findFirstByUserIdAndDomain(userId, normalizeDomain(request.domain()));
        if (existing.isPresent()) {
            TrackedWebsite w = existing.get();
            if (w.getFromAnalysisId() == null) w.setFromAnalysisId(analysisId);
            w.setLastInspectionAt(Instant.now());
            if (request.riskLevel() != null) w.setRiskLevel(trimToNull(request.riskLevel()));
            if (request.dataCategories() != null) w.setDataCategories(trimToNull(request.dataCategories()));
            if (request.trackingTechnologies() != null) w.setTrackingTechnologies(trimToNull(request.trackingTechnologies()));
            if (request.category() != null) w.setCategory(trimToNull(request.category()));
            TrackedWebsite saved = repository.save(w);
            privacyDiscoveryService.applyToWebsite(userId, saved.getDomain());
            return saved;
        }
        TrackedWebsite w = TrackedWebsite.builder()
                .userId(userId)
                .name(request.name().trim())
                .domain(normalizeDomain(request.domain()))
                .url(request.url().trim())
                .category(trimToNull(request.category()))
                .riskLevel(trimToNull(request.riskLevel()))
                .dataCategories(trimToNull(request.dataCategories()))
                .trackingTechnologies(trimToNull(request.trackingTechnologies()))
                .fromAnalysisId(analysisId)
                .lastInspectionAt(Instant.now())
                .firstRecordedAt(Instant.now())
                .build();
        TrackedWebsite saved = repository.save(w);
        privacyDiscoveryService.applyToWebsite(userId, saved.getDomain());
        return saved;
    }

    @CacheEvict(cacheNames = "digitalFootprintMetrics", key = "#userId")
    @Transactional
    public TrackedWebsite update(String userId, UUID id, UpdateTrackedWebsiteRequest request) {
        TrackedWebsite w = requireOwned(userId, id);
        if (request.name() != null) w.setName(request.name().trim());
        if (request.domain() != null) w.setDomain(normalizeDomain(request.domain()));
        if (request.url() != null) w.setUrl(request.url().trim());
        if (request.category() != null) w.setCategory(trimToNull(request.category()));
        if (request.accountStatus() != null) w.setAccountStatus(trimToNull(request.accountStatus()));
        if (request.privacyPolicyUrl() != null) w.setPrivacyPolicyUrl(trimToNull(request.privacyPolicyUrl()));
        if (request.deletionPolicyUrl() != null) w.setDeletionPolicyUrl(trimToNull(request.deletionPolicyUrl()));
        if (request.riskLevel() != null) w.setRiskLevel(trimToNull(request.riskLevel()));
        if (request.dataCategories() != null) w.setDataCategories(trimToNull(request.dataCategories()));
        if (request.trackingTechnologies() != null) w.setTrackingTechnologies(trimToNull(request.trackingTechnologies()));
        if (request.deletionRequestStatus() != null) w.setDeletionRequestStatus(trimToNull(request.deletionRequestStatus()));
        if (request.fromAnalysisId() != null) w.setFromAnalysisId(Ids.parseNullableUuid(request.fromAnalysisId(), "fromAnalysisId"));
        return repository.save(w);
    }

    @Transactional(readOnly = true)
    public TrackedWebsite get(String userId, UUID id) {
        return requireOwned(userId, id);
    }

    @Transactional(readOnly = true)
    public List<TrackedWebsite> search(String userId, String query, String category, String riskLevel, String deletionStatus, String sort) {
        String q = query == null ? null : query.trim().toLowerCase(Locale.ROOT);
        List<TrackedWebsite> results = repository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(w -> q == null || q.isBlank()
                        || w.getName().toLowerCase(Locale.ROOT).contains(q)
                        || w.getDomain().toLowerCase(Locale.ROOT).contains(q))
                .filter(w -> blank(category) || category.equalsIgnoreCase(w.getCategory()))
                .filter(w -> blank(riskLevel) || riskLevel.equalsIgnoreCase(w.getRiskLevel()))
                .filter(w -> blank(deletionStatus) || deletionStatus.equalsIgnoreCase(w.getDeletionRequestStatus()))
                .toList();

        if ("risk".equalsIgnoreCase(sort)) {
            return results.stream()
                    .sorted(Comparator.comparingInt((TrackedWebsite w) -> riskRank(w.getRiskLevel())).reversed()
                            .thenComparing(TrackedWebsite::getCreatedAt, Comparator.reverseOrder()))
                    .toList();
        }
        return results;
    }

    @Cacheable(cacheNames = "digitalFootprintMetrics", key = "#userId")
    @Transactional(readOnly = true)
    public Map<String, Object> metrics(String userId) {
        List<TrackedWebsite> all = repository.findAllByUserIdOrderByCreatedAtDesc(userId);
        List<PersonalDataRecord> records = dataRecordRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        long sensitive = records.stream()
                .filter(r -> SENSITIVE_CATEGORIES.stream().anyMatch(s -> r.getDataCategory().toUpperCase(Locale.ROOT).contains(s)))
                .map(PersonalDataRecord::getTrackedWebsiteId)
                .distinct()
                .count();
        long trackedWithTrackers = all.stream()
                .filter(w -> !blank(w.getTrackingTechnologies()))
                .count();
        long trackerCookies = cookieRepository.countByUserIdAndTrackingRelatedTrue(userId);
        long pending = privacyRequestRepository.countByUserIdAndStatusIn(userId, PENDING_STATUSES);
        long completed = privacyRequestRepository.countByUserIdAndStatus(userId, PrivacyRequestStatus.COMPLETED.name());
        long needsApproval = privacyRequestRepository.countByUserIdAndStatusIn(userId, List.of(
                PrivacyRequestStatus.READY_FOR_REVIEW.name(),
                PrivacyRequestStatus.AWAITING_USER_APPROVAL.name()));

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("totalWebsites", all.size());
        metrics.put("sensitiveWebsites", sensitive);
        metrics.put("websitesWithTrackers", trackedWithTrackers);
        metrics.put("trackerCookies", trackerCookies);
        metrics.put("trackedCookies", cookieRepository.countByUserId(userId));
        metrics.put("pendingDeletions", pending);
        metrics.put("completedDeletions", completed);
        metrics.put("awaitingApproval", needsApproval);
        metrics.put("outstandingActions", pending + needsApproval);
        return metrics;
    }

    @Transactional(readOnly = true)
    public Page<TrackedWebsite> list(String userId, int page, int size) {
        if (page < 0) throw new InvalidRequestException("page must be >= 0");
        if (size < 1 || size > 100) throw new InvalidRequestException("size must be between 1 and 100");
        return repository.findAllByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    }

    @CacheEvict(cacheNames = "digitalFootprintMetrics", key = "#userId")
    @Transactional
    public void delete(String userId, UUID id) {
        TrackedWebsite w = requireOwned(userId, id);
        repository.delete(w);
    }

    @Transactional(readOnly = true)
    public TrackedWebsite requireOwned(String userId, UUID id) {
        return repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tracked website not found"));
    }

    @Transactional(readOnly = true)
    public TrackedWebsite requireOwnedOrNull(String userId, UUID id) {
        return id == null ? null : requireOwned(userId, id);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    static String normalizeDomain(String domain) {
        if (domain == null) throw new InvalidRequestException("domain is required");
        String d = domain.trim().toLowerCase(Locale.ROOT);
        d = d.replaceFirst("^https?://", "");
        int slash = d.indexOf('/');
        if (slash >= 0) d = d.substring(0, slash);
        d = d.replaceFirst("^www\\.", "");
        if (d.isBlank()) throw new InvalidRequestException("domain is required");
        return d;
    }

    private static int riskRank(String risk) {
        if (risk == null) return 0;
        return switch (risk.toUpperCase(Locale.ROOT)) {
            case "HIGH", "CRITICAL" -> 3;
            case "MEDIUM" -> 2;
            case "LOW" -> 1;
            default -> 0;
        };
    }
}
