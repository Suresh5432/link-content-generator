package com.linklens.summarizationservice.privacyassistant.controller;

import com.linklens.summarizationservice.privacyassistant.PrivacyRequestDraftRequest;
import com.linklens.summarizationservice.privacyassistant.PrivacyRequestDraftResponse;
import com.linklens.summarizationservice.privacyassistant.PrivacyRequestDraftService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/privacy-request-drafts")
public class PrivacyRequestDraftController {

    private final PrivacyRequestDraftService service;

    @PostMapping
    public PrivacyRequestDraftResponse draft(@Valid @RequestBody PrivacyRequestDraftRequest request) {
        return new PrivacyRequestDraftResponse(service.draft(request.requestType(), request.websiteName(), request.dataCategories()));
    }
}
