package com.linklens.userservice.digitalfootprint.service;

import com.linklens.userservice.digitalfootprint.dto.UpdatePrivacyPreferencesRequest;
import com.linklens.userservice.digitalfootprint.entity.UserPrivacyPreference;
import com.linklens.userservice.digitalfootprint.repo.UserPrivacyPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PrivacyPreferenceService {

    private final UserPrivacyPreferenceRepository repository;

    @Transactional
    public UserPrivacyPreference getOrCreate(String userId) {
        return repository.findByUserId(userId).orElseGet(() -> repository.save(
                UserPrivacyPreference.builder().userId(userId).build()));
    }

    @Transactional
    public UserPrivacyPreference update(String userId, UpdatePrivacyPreferencesRequest request) {
        UserPrivacyPreference pref = getOrCreate(userId);
        if (request.analyticsCookies() != null) pref.setAnalyticsCookies(request.analyticsCookies());
        if (request.advertisingCookies() != null) pref.setAdvertisingCookies(request.advertisingCookies());
        if (request.marketingCommunications() != null) pref.setMarketingCommunications(request.marketingCommunications());
        if (request.thirdPartyTracking() != null) pref.setThirdPartyTracking(request.thirdPartyTracking());
        if (request.personalizedAdvertising() != null) pref.setPersonalizedAdvertising(request.personalizedAdvertising());
        if (request.dataSharing() != null) pref.setDataSharing(request.dataSharing());
        return repository.save(pref);
    }
}
