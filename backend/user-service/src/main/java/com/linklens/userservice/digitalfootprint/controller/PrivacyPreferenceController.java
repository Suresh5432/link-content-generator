package com.linklens.userservice.digitalfootprint.controller;

import com.linklens.userservice.digitalfootprint.dto.UpdatePrivacyPreferencesRequest;
import com.linklens.userservice.digitalfootprint.entity.UserPrivacyPreference;
import com.linklens.userservice.digitalfootprint.service.PrivacyPreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/privacy-preferences")
public class PrivacyPreferenceController {

    private final PrivacyPreferenceService service;

    @GetMapping
    public UserPrivacyPreference get(@AuthenticationPrincipal Jwt jwt) {
        return service.getOrCreate(jwt.getSubject());
    }

    @PutMapping
    public UserPrivacyPreference update(@AuthenticationPrincipal Jwt jwt, @RequestBody UpdatePrivacyPreferencesRequest request) {
        return service.update(jwt.getSubject(), request);
    }
}
