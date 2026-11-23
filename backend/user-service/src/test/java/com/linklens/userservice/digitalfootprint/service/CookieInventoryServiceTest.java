package com.linklens.userservice.digitalfootprint.service;

import com.linklens.userservice.digitalfootprint.dto.CookieMetadata;
import com.linklens.userservice.digitalfootprint.dto.SyncCookiesRequest;
import com.linklens.userservice.digitalfootprint.entity.BrowserIntegration;
import com.linklens.userservice.digitalfootprint.entity.CookieInventory;
import com.linklens.userservice.digitalfootprint.exception.InvalidRequestException;
import com.linklens.userservice.digitalfootprint.exception.ResourceNotFoundException;
import com.linklens.userservice.digitalfootprint.repo.BrowserIntegrationRepository;
import com.linklens.userservice.digitalfootprint.repo.BrowserSyncHistoryRepository;
import com.linklens.userservice.digitalfootprint.repo.CookieInventoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CookieInventoryServiceTest {

    @Mock CookieInventoryRepository repository;
    @Mock BrowserIntegrationRepository integrationRepository;
    @Mock BrowserSyncHistoryRepository syncHistoryRepository;
    @InjectMocks CookieInventoryService service;

    private static final String USER = "user-1";

    private BrowserIntegration integration(String installationId) {
        return BrowserIntegration.builder().id(UUID.randomUUID()).userId(USER).installationId(installationId).build();
    }

    @Test
    void syncRequiresOwnedIntegration() {
        SyncCookiesRequest request = new SyncCookiesRequest("inst-1", List.of(new CookieMetadata("sid", "example.com", true, null, null, "ESSENTIAL", true, true, "Lax", false)));
        when(integrationRepository.findByUserIdAndInstallationId(USER, "inst-1")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.sync(USER, request));
        verify(repository, never()).save(any());
    }

    @Test
    void syncStoresMetadataAndFlagsTrackingCategories() {
        BrowserIntegration integration = integration("inst-1");
        when(integrationRepository.findByUserIdAndInstallationId(USER, "inst-1")).thenReturn(Optional.of(integration));
        when(repository.findByUserIdAndBrowserIntegrationIdAndDomainAndCookieName(eq(USER), eq(integration.getId()), any(), any()))
                .thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SyncCookiesRequest request = new SyncCookiesRequest("inst-1", List.of(
                new CookieMetadata("_ga", "www.example.com", true, null, "Analytics", "ANALYTICS", true, false, "Lax", false),
                new CookieMetadata("session", "other.com", true, null, "Sign-in", "ESSENTIAL", true, true, "Strict", false)));

        CookieInventoryService.SyncResult result = service.sync(USER, request);

        assertEquals(2, result.cookiesStored());
        assertEquals(2, result.domains());
        verify(repository, times(2)).save(any());
        verify(syncHistoryRepository).save(any());
        verify(integrationRepository).save(integration);
        assertNotNull(integration.getLastSyncAt());
    }

    @Test
    void syncRejectsOversizedBatch() {
        List<CookieMetadata> cookies = java.util.stream.IntStream.range(0, 5001)
                .mapToObj(i -> new CookieMetadata("c" + i, "example.com", true, null, null, "UNKNOWN", false, false, null, false))
                .toList();
        assertThrows(InvalidRequestException.class, () -> service.sync(USER, new SyncCookiesRequest("inst-1", cookies)));
    }

    @Test
    void deletingMetadataNeverClaimsBrowserRemoval() {
        CookieInventory cookie = CookieInventory.builder().id(UUID.randomUUID()).userId(USER).cookieName("_ga").domain("example.com").build();
        when(repository.findByIdAndUserId(cookie.getId(), USER)).thenReturn(Optional.of(cookie));

        String message = service.deleteMetadata(USER, cookie.getId());

        assertTrue(message.toLowerCase().contains("remains in your browser"));
        verify(repository).delete(cookie);
    }

    @Test
    void deletingAnotherUsersCookieIsDenied() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdAndUserId(id, USER)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.deleteMetadata(USER, id));
    }
}
