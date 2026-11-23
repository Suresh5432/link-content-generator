package com.linklens.summarizationservice.privacyassistant.controller;

import com.linklens.summarizationservice.privacyassistant.PrivacyAssistantRequest;
import com.linklens.summarizationservice.privacyassistant.PrivacyAssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/privacy-assistant")
public class PrivacyAssistantController {

    private final PrivacyAssistantService service;

    @PostMapping
    public Map<String, String> ask(@Valid @RequestBody PrivacyAssistantRequest request,
                                   @AuthenticationPrincipal Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null || jwt.getSubject().isBlank()) {
            throw new IllegalArgumentException("An authenticated user is required");
        }
        return Map.of("answer", service.answer(request.question(), jwt.getSubject()));
    }
}
