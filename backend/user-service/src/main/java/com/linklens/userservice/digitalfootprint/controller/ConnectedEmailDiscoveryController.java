package com.linklens.userservice.digitalfootprint.controller;

import com.linklens.userservice.digitalfootprint.entity.ConnectedEmailAccount;
import com.linklens.userservice.digitalfootprint.entity.ConnectedEmailDiscovery;
import com.linklens.userservice.digitalfootprint.service.ConnectedEmailDiscoveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/digital-footprint/connected-email")
public class ConnectedEmailDiscoveryController {

    private final ConnectedEmailDiscoveryService service;

    @GetMapping("/accounts")
    public List<ConnectedEmailAccount> accounts(@AuthenticationPrincipal Jwt jwt) {
        return service.accounts(jwt.getSubject());
    }

    @GetMapping("/discoveries")
    public List<ConnectedEmailDiscovery> discoveries(@AuthenticationPrincipal Jwt jwt) {
        return service.discoveries(jwt.getSubject());
    }

    @PostMapping("/accounts")
    public ConnectedEmailAccount register(@AuthenticationPrincipal Jwt jwt, @RequestBody Map<String, String> body) {
        return service.register(jwt.getSubject(), body.get("provider"), body.get("accountEmail"), body.get("scopes"));
    }

    @PostMapping("/accounts/{id}/consent")
    public ConnectedEmailAccount consent(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.recordConsent(jwt.getSubject(), id);
    }

    @DeleteMapping("/accounts/{id}")
    public void disconnect(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        service.disconnect(jwt.getSubject(), id);
    }
}
