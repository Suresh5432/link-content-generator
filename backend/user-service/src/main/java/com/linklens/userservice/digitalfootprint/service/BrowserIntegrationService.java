package com.linklens.userservice.digitalfootprint.service;

import com.linklens.userservice.digitalfootprint.dto.RegisterBrowserIntegrationRequest;
import com.linklens.userservice.digitalfootprint.entity.BrowserIntegration;
import com.linklens.userservice.digitalfootprint.entity.BrowserSyncHistory;
import com.linklens.userservice.digitalfootprint.exception.ResourceNotFoundException;
import com.linklens.userservice.digitalfootprint.repo.BrowserIntegrationRepository;
import com.linklens.userservice.digitalfootprint.repo.BrowserSyncHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BrowserIntegrationService {

    private final BrowserIntegrationRepository repository;
    private final BrowserSyncHistoryRepository syncHistoryRepository;

    @Transactional
    public BrowserIntegration register(String userId, RegisterBrowserIntegrationRequest request) {
        BrowserIntegration integration = repository.findByUserIdAndInstallationId(userId, request.installationId().trim())
                .orElseGet(() -> BrowserIntegration.builder()
                        .userId(userId)
                        .installationId(request.installationId().trim())
                        .build());
        if (request.browserName() != null) integration.setBrowserName(trimToNull(request.browserName()));
        if (request.extensionVersion() != null) integration.setExtensionVersion(trimToNull(request.extensionVersion()));
        if (request.deviceLabel() != null) integration.setDeviceLabel(trimToNull(request.deviceLabel()));
        return repository.save(integration);
    }

    @Transactional(readOnly = true)
    public List<BrowserIntegration> list(String userId) {
        return repository.findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<BrowserSyncHistory> history(String userId, UUID integrationId) {
        requireOwned(userId, integrationId);
        return syncHistoryRepository.findAllByIntegrationIdAndUserIdOrderByCreatedAtDesc(integrationId, userId);
    }

    @Transactional
    public void delete(String userId, UUID id) {
        repository.delete(requireOwned(userId, id));
    }

    @Transactional
    public void markSynced(String userId, UUID id) {
        BrowserIntegration integration = requireOwned(userId, id);
        integration.setLastSyncAt(Instant.now());
        repository.save(integration);
    }

    @Transactional(readOnly = true)
    public BrowserIntegration requireOwned(String userId, UUID id) {
        return repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Browser integration not found"));
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
