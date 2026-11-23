package com.linklens.userservice.digitalfootprint.controller;

import com.linklens.userservice.digitalfootprint.dto.CreateEvidenceRequest;
import com.linklens.userservice.digitalfootprint.dto.CreatePrivacyRequest;
import com.linklens.userservice.digitalfootprint.dto.UpdatePrivacyRequestRequest;
import com.linklens.userservice.digitalfootprint.entity.PrivacyRequest;
import com.linklens.userservice.digitalfootprint.entity.PrivacyRequestEvidence;
import com.linklens.userservice.digitalfootprint.entity.PrivacyRequestHistory;
import com.linklens.userservice.digitalfootprint.service.PrivacyRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/privacy-requests")
public class PrivacyRequestController {

    private final PrivacyRequestService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PrivacyRequest create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreatePrivacyRequest request) {
        return service.create(jwt.getSubject(), request);
    }

    @GetMapping
    public List<PrivacyRequest> list(@AuthenticationPrincipal Jwt jwt) {
        return service.list(jwt.getSubject());
    }

    @GetMapping("/reminders")
    public List<PrivacyRequest> reminders(@AuthenticationPrincipal Jwt jwt) {
        return service.dueReminders(jwt.getSubject());
    }

    @GetMapping("/{id}")
    public PrivacyRequest get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.get(jwt.getSubject(), id);
    }

    @PatchMapping("/{id}")
    public PrivacyRequest update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @RequestBody UpdatePrivacyRequestRequest request) {
        return service.update(jwt.getSubject(), id, request);
    }

    @PostMapping("/{id}/review")
    public PrivacyRequest review(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.requestReview(jwt.getSubject(), id);
    }

    @PostMapping("/{id}/approve")
    public PrivacyRequest approve(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.approve(jwt.getSubject(), id);
    }

    @PostMapping("/{id}/submit")
    public PrivacyRequest submit(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                 @RequestBody(required = false) Map<String, String> body) {
        Map<String, String> safe = body == null ? Map.of() : body;
        return service.submit(jwt.getSubject(), id, safe.get("submissionMethod"), safe.get("externalReference"));
    }

    @PostMapping("/{id}/cancel")
    public PrivacyRequest cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.cancel(jwt.getSubject(), id);
    }

    @PatchMapping("/{id}/status")
    public PrivacyRequest updateStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                       @RequestBody Map<String, String> body) {
        return service.updateStatus(jwt.getSubject(), id, body.get("status"), body.get("note"));
    }

    @PostMapping("/{id}/response")
    public PrivacyRequest recordResponse(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                         @RequestBody Map<String, Object> body) {
        String response = body.get("response") == null ? null : String.valueOf(body.get("response"));
        boolean claimed = Boolean.TRUE.equals(body.get("websiteClaimedDeletion"));
        return service.recordResponse(jwt.getSubject(), id, response, claimed);
    }

    @PostMapping("/{id}/verify")
    public PrivacyRequest verify(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                 @RequestBody Map<String, Object> body) {
        boolean verified = Boolean.TRUE.equals(body.get("userVerified"));
        return service.verifyCompletion(jwt.getSubject(), id, verified);
    }

    @PostMapping("/{id}/evidence")
    @ResponseStatus(HttpStatus.CREATED)
    public PrivacyRequestEvidence addEvidence(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                              @Valid @RequestBody CreateEvidenceRequest request) {
        return service.addEvidence(jwt.getSubject(), id, request);
    }

    @GetMapping("/{id}/evidence")
    public List<PrivacyRequestEvidence> evidence(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.evidence(jwt.getSubject(), id);
    }

    @GetMapping("/{id}/history")
    public List<PrivacyRequestHistory> history(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.history(jwt.getSubject(), id);
    }

    @PostMapping("/{id}/reminder-sent")
    public PrivacyRequest reminderSent(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.markReminderSent(jwt.getSubject(), id);
    }
}
