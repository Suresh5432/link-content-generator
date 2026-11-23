package com.linklens.userservice.digitalfootprint.controller;

import com.linklens.userservice.digitalfootprint.dto.RegisterBrowserIntegrationRequest;
import com.linklens.userservice.digitalfootprint.dto.SyncCookiesRequest;
import com.linklens.userservice.digitalfootprint.entity.BrowserIntegration;
import com.linklens.userservice.digitalfootprint.entity.BrowserSyncHistory;
import com.linklens.userservice.digitalfootprint.service.BrowserIntegrationService;
import com.linklens.userservice.digitalfootprint.service.CookieInventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/browser-integrations")
public class BrowserIntegrationController {

    private final BrowserIntegrationService integrationService;
    private final CookieInventoryService cookieService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public BrowserIntegration register(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody RegisterBrowserIntegrationRequest request) {
        return integrationService.register(jwt.getSubject(), request);
    }

    @GetMapping
    public List<BrowserIntegration> list(@AuthenticationPrincipal Jwt jwt) {
        return integrationService.list(jwt.getSubject());
    }

    @PostMapping("/sync")
    public CookieInventoryService.SyncResult sync(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SyncCookiesRequest request) {
        return cookieService.sync(jwt.getSubject(), request);
    }

    @GetMapping("/{id}/history")
    public List<BrowserSyncHistory> history(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return integrationService.history(jwt.getSubject(), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        integrationService.delete(jwt.getSubject(), id);
    }
}
