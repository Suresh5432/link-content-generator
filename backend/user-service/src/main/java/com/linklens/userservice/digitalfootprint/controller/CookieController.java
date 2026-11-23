package com.linklens.userservice.digitalfootprint.controller;

import com.linklens.userservice.digitalfootprint.entity.CookieInventory;
import com.linklens.userservice.digitalfootprint.service.CookieInventoryService;
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
@RequestMapping("/api/v1/cookies")
public class CookieController {

    private final CookieInventoryService service;

    @GetMapping
    public List<CookieInventory> list(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) String domain) {
        return service.list(jwt.getSubject(), domain);
    }

    @GetMapping("/domains")
    public List<String> domains(@AuthenticationPrincipal Jwt jwt) {
        return service.domains(jwt.getSubject());
    }

    @GetMapping("/tracking")
    public List<CookieInventory> tracking(@AuthenticationPrincipal Jwt jwt) {
        return service.trackingCookies(jwt.getSubject());
    }

    /**
     * Removes only the stored metadata. A server cannot delete cookies from a
     * remote browser; the extension performs removal with user confirmation.
     */
    @DeleteMapping("/inventory/{cookieId}")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID cookieId) {
        return Map.of("message", service.deleteMetadata(jwt.getSubject(), cookieId));
    }
}
