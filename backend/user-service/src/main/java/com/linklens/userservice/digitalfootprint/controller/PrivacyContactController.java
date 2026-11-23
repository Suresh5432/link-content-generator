package com.linklens.userservice.digitalfootprint.controller;

import com.linklens.userservice.digitalfootprint.dto.UpdatePrivacyContactRequest;
import com.linklens.userservice.digitalfootprint.entity.WebsitePrivacyContact;
import com.linklens.userservice.digitalfootprint.service.WebsitePrivacyContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/digital-footprint")
public class PrivacyContactController {

    private final WebsitePrivacyContactService service;

    @PutMapping("/websites/{websiteId}/privacy-contact")
    public WebsitePrivacyContact upsert(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID websiteId, @RequestBody UpdatePrivacyContactRequest request) {
        return service.upsert(jwt.getSubject(), websiteId, request);
    }

    @GetMapping("/websites/{websiteId}/privacy-contact")
    public WebsitePrivacyContact get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID websiteId) {
        return service.get(jwt.getSubject(), websiteId);
    }

    @GetMapping("/privacy-contacts")
    public List<WebsitePrivacyContact> list(@AuthenticationPrincipal Jwt jwt) {
        return service.list(jwt.getSubject());
    }
}
