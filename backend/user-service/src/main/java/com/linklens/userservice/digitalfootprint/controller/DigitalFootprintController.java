package com.linklens.userservice.digitalfootprint.controller;

import com.linklens.userservice.digitalfootprint.dto.CreateTrackedWebsiteRequest;
import com.linklens.userservice.digitalfootprint.dto.ImportWebsiteRequest;
import com.linklens.userservice.digitalfootprint.dto.UpdateTrackedWebsiteRequest;
import com.linklens.userservice.digitalfootprint.entity.TrackedWebsite;
import com.linklens.userservice.digitalfootprint.service.DigitalFootprintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/digital-footprint/websites")
public class DigitalFootprintController {

    private final DigitalFootprintService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TrackedWebsite create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateTrackedWebsiteRequest request) {
        return service.create(jwt.getSubject(), request);
    }

    @PostMapping("/import")
    @ResponseStatus(HttpStatus.CREATED)
    public TrackedWebsite importFromAnalysis(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ImportWebsiteRequest request) {
        return service.importFromAnalysis(jwt.getSubject(), request);
    }

    @GetMapping
    public Page<TrackedWebsite> list(@AuthenticationPrincipal Jwt jwt,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        return service.list(jwt.getSubject(), page, size);
    }

    @GetMapping("/search")
    public List<TrackedWebsite> search(@AuthenticationPrincipal Jwt jwt,
                                       @RequestParam(required = false) String query,
                                       @RequestParam(required = false) String category,
                                       @RequestParam(required = false) String riskLevel,
                                       @RequestParam(required = false) String deletionStatus,
                                       @RequestParam(required = false) String sort) {
        return service.search(jwt.getSubject(), query, category, riskLevel, deletionStatus, sort);
    }

    @GetMapping("/metrics")
    public Map<String, Object> metrics(@AuthenticationPrincipal Jwt jwt) {
        return service.metrics(jwt.getSubject());
    }

    @GetMapping("/{id}")
    public TrackedWebsite get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.get(jwt.getSubject(), id);
    }

    @PatchMapping("/{id}")
    public TrackedWebsite update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @RequestBody UpdateTrackedWebsiteRequest request) {
        return service.update(jwt.getSubject(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        service.delete(jwt.getSubject(), id);
    }
}
