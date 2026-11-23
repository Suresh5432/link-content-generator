package com.linklens.userservice.digitalfootprint.controller;

import com.linklens.userservice.digitalfootprint.service.PrivacyExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/digital-footprint")
public class DigitalFootprintAccountController {

    private final PrivacyExportService service;

    @GetMapping("/export")
    public Map<String, Object> export(@AuthenticationPrincipal Jwt jwt) {
        return service.export(jwt.getSubject());
    }

    @DeleteMapping("/account-data")
    public Map<String, Object> deleteAll(@AuthenticationPrincipal Jwt jwt) {
        return service.deleteAllFootprintData(jwt.getSubject());
    }
}
