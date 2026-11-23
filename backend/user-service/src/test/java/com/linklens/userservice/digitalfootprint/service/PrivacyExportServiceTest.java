package com.linklens.userservice.digitalfootprint.service;

import com.linkcontext.contracts.EventRoutingKey;
import com.linklens.userservice.digitalfootprint.messaging.PrivacyEventPublisher;
import com.linklens.userservice.digitalfootprint.repo.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrivacyExportServiceTest {

    @Mock TrackedWebsiteRepository websiteRepository;
    @Mock UserWebsiteAccountRepository accountRepository;
    @Mock PersonalDataRecordRepository dataRecordRepository;
    @Mock CookieInventoryRepository cookieRepository;
    @Mock WebsitePrivacyContactRepository contactRepository;
    @Mock UserPrivacyPreferenceRepository preferenceRepository;
    @Mock PrivacyRequestRepository requestRepository;
    @Mock PrivacyRequestEvidenceRepository evidenceRepository;
    @Mock PrivacyRequestHistoryRepository historyRepository;
    @Mock BrowserIntegrationRepository integrationRepository;
    @Mock PrivacyDiscoveryFindingRepository discoveryFindingRepository;
    @Mock PrivacyEventOutboxRepository outboxRepository;
    @Mock ConnectedEmailAccountRepository connectedEmailAccountRepository;
    @Mock ConnectedEmailDiscoveryRepository connectedEmailDiscoveryRepository;
    @Mock PrivacyEventPublisher eventPublisher;
    @InjectMocks PrivacyExportService service;

    @Test
    void exportIncludesScopeAndRetention() {
        when(requestRepository.findAllByUserIdOrderByCreatedAtDesc("u1")).thenReturn(List.of());
        when(websiteRepository.findAllByUserIdOrderByCreatedAtDesc("u1")).thenReturn(List.of());
        when(accountRepository.findAllByUserIdOrderByCreatedAtDesc("u1")).thenReturn(List.of());
        when(dataRecordRepository.findAllByUserIdOrderByCreatedAtDesc("u1")).thenReturn(List.of());
        when(cookieRepository.findAllByUserIdOrderByDomainAscCookieNameAsc("u1")).thenReturn(List.of());
        when(contactRepository.findAllByUserIdOrderByUpdatedAtDesc("u1")).thenReturn(List.of());
        when(preferenceRepository.findByUserId("u1")).thenReturn(Optional.empty());
        when(integrationRepository.findAllByUserIdOrderByCreatedAtDesc("u1")).thenReturn(List.of());
        when(discoveryFindingRepository.findAllByUserIdOrderByDomainAscCreatedAtAsc("u1")).thenReturn(List.of());
        when(outboxRepository.findAllByUserIdOrderByCreatedAtDesc("u1")).thenReturn(List.of());
        when(connectedEmailAccountRepository.findAllByUserIdOrderByCreatedAtDesc("u1")).thenReturn(List.of());
        when(connectedEmailDiscoveryRepository.findAllByUserIdOrderByLastSeenAtDesc("u1")).thenReturn(List.of());

        Map<String, Object> export = service.export("u1");

        assertEquals("u1", export.get("userId"));
        assertTrue(export.containsKey("scope"));
        assertTrue(String.valueOf(export.get("retentionNotice")).contains("footprint records"));
    }

    @Test
    void deleteRecordsDeletionEventAndReturnsScope() {
        when(requestRepository.findAllByUserIdOrderByCreatedAtDesc("u1")).thenReturn(List.of());
        when(dataRecordRepository.findAllByUserIdOrderByCreatedAtDesc("u1")).thenReturn(List.of());
        when(accountRepository.findAllByUserIdOrderByCreatedAtDesc("u1")).thenReturn(List.of());
        when(cookieRepository.findAllByUserIdOrderByDomainAscCookieNameAsc("u1")).thenReturn(List.of());
        when(contactRepository.findAllByUserIdOrderByUpdatedAtDesc("u1")).thenReturn(List.of());
        when(preferenceRepository.findByUserId("u1")).thenReturn(Optional.empty());
        when(integrationRepository.findAllByUserIdOrderByCreatedAtDesc("u1")).thenReturn(List.of());
        when(discoveryFindingRepository.findAllByUserIdOrderByDomainAscCreatedAtAsc("u1")).thenReturn(List.of());
        when(outboxRepository.findAllByUserIdOrderByCreatedAtDesc("u1")).thenReturn(List.of());
        when(connectedEmailDiscoveryRepository.findAllByUserIdOrderByLastSeenAtDesc("u1")).thenReturn(List.of());
        when(connectedEmailAccountRepository.findAllByUserIdOrderByCreatedAtDesc("u1")).thenReturn(List.of());
        when(websiteRepository.findAllByUserIdOrderByCreatedAtDesc("u1")).thenReturn(List.of());

        Map<String, Object> result = service.deleteAllFootprintData("u1");

        assertEquals("DIGITAL_FOOTPRINT", ((Map<?, ?>) result.get("scope")).get("dataClass"));
        ArgumentCaptor<String> routing = ArgumentCaptor.forClass(String.class);
        verify(eventPublisher).record(eq(EventRoutingKey.USER_FOOTPRINT_DELETED), any());
        assertNotNull(result.get("deletedAt"));
    }
}
