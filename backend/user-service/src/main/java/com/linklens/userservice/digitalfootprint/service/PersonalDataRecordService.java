package com.linklens.userservice.digitalfootprint.service;

import com.linklens.userservice.digitalfootprint.domain.PrivacyDataSource;
import com.linklens.userservice.digitalfootprint.dto.CreatePersonalDataRecordRequest;
import com.linklens.userservice.digitalfootprint.dto.UpdatePersonalDataRecordRequest;
import com.linklens.userservice.digitalfootprint.entity.PersonalDataRecord;
import com.linklens.userservice.digitalfootprint.exception.InvalidRequestException;
import com.linklens.userservice.digitalfootprint.exception.ResourceNotFoundException;
import com.linklens.userservice.digitalfootprint.repo.PersonalDataRecordRepository;
import com.linklens.userservice.digitalfootprint.repo.TrackedWebsiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PersonalDataRecordService {

    private final PersonalDataRecordRepository repository;
    private final TrackedWebsiteRepository websiteRepository;

    @Transactional
    public PersonalDataRecord create(String userId, UUID websiteId, CreatePersonalDataRecordRequest request) {
        websiteRepository.findByIdAndUserId(websiteId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tracked website not found"));
        SensitiveDataGuard.rejectSecretMaterial(request.dataType(), request.description());
        PrivacyDataSource source = PrivacyDataSource.parse(request.source());
        PersonalDataRecord r = PersonalDataRecord.builder()
                .trackedWebsiteId(websiteId)
                .userId(userId)
                .dataCategory(request.dataCategory().trim())
                .dataType(request.dataType().trim())
                .description(trimToNull(request.description()))
                .source(source.name())
                .confirmedByUser(source == PrivacyDataSource.USER_CONFIRMED)
                .observedOnWebsite(source == PrivacyDataSource.OBSERVED_ON_WEBSITE)
                .declaredInPolicy(source == PrivacyDataSource.DECLARED_IN_POLICY)
                .inferredByAi(source == PrivacyDataSource.INFERRED_BY_AI)
                .dateShared(Ids.parseDate(request.dateShared(), "dateShared"))
                .evidenceSource(trimToNull(request.evidenceSource()))
                .lastVerifiedAt(source == PrivacyDataSource.USER_CONFIRMED ? Instant.now() : null)
                .build();
        return repository.save(r);
    }

    @Transactional(readOnly = true)
    public List<PersonalDataRecord> list(String userId, UUID websiteId) {
        websiteRepository.findByIdAndUserId(websiteId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tracked website not found"));
        return repository.findAllByTrackedWebsiteIdAndUserIdOrderByCreatedAtDesc(websiteId, userId);
    }

    @Transactional(readOnly = true)
    public List<PersonalDataRecord> listAll(String userId) {
        return repository.findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public PersonalDataRecord update(String userId, UUID recordId, UpdatePersonalDataRecordRequest request) {
        PersonalDataRecord r = repository.findByIdAndUserId(recordId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Personal data record not found"));
        if (request.dataType() != null) SensitiveDataGuard.rejectSecretMaterial(request.dataType(), request.description());
        if (request.dataCategory() != null) r.setDataCategory(request.dataCategory().trim());
        if (request.dataType() != null) r.setDataType(request.dataType().trim());
        if (request.description() != null) r.setDescription(trimToNull(request.description()));
        if (request.evidenceSource() != null) r.setEvidenceSource(trimToNull(request.evidenceSource()));
        if (request.dateShared() != null) r.setDateShared(Ids.parseDate(request.dateShared(), "dateShared"));
        if (request.source() != null) {
            PrivacyDataSource source = PrivacyDataSource.parse(request.source());
            r.setSource(source.name());
            r.setConfirmedByUser(source == PrivacyDataSource.USER_CONFIRMED);
            r.setObservedOnWebsite(source == PrivacyDataSource.OBSERVED_ON_WEBSITE);
            r.setDeclaredInPolicy(source == PrivacyDataSource.DECLARED_IN_POLICY);
            r.setInferredByAi(source == PrivacyDataSource.INFERRED_BY_AI);
            r.setLastVerifiedAt(source == PrivacyDataSource.USER_CONFIRMED ? Instant.now() : r.getLastVerifiedAt());
        }
        if (request.confirmedByUser() != null) {
            r.setConfirmedByUser(request.confirmedByUser());
            if (request.confirmedByUser()) r.setLastVerifiedAt(Instant.now());
        }
        return repository.save(r);
    }

    @Transactional
    public void delete(String userId, UUID id) {
        PersonalDataRecord r = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Personal data record not found"));
        repository.delete(r);
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
