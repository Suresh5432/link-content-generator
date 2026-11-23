package com.linklens.userservice.digitalfootprint.service;

import com.linkcontext.contracts.events.PrivacyDiscoveryDiscoveredEvent;
import com.linklens.userservice.digitalfootprint.dto.UpdatePrivacyContactRequest;
import com.linklens.userservice.digitalfootprint.entity.PrivacyDiscoveryFinding;
import com.linklens.userservice.digitalfootprint.entity.TrackedWebsite;
import com.linklens.userservice.digitalfootprint.repo.PrivacyDiscoveryFindingRepository;
import com.linklens.userservice.digitalfootprint.repo.TrackedWebsiteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrivacyDiscoveryServiceTest {

    @Mock PrivacyDiscoveryFindingRepository findingRepository;
    @Mock TrackedWebsiteRepository websiteRepository;
    @Mock WebsitePrivacyContactService contactService;
    @InjectMocks PrivacyDiscoveryService service;

    private PrivacyDiscoveryDiscoveredEvent event(int version, String userId) {
        return new PrivacyDiscoveryDiscoveredEvent(UUID.randomUUID(), UUID.randomUUID(), userId,
                "Example.com", "https://example.com", List.of(
                new PrivacyDiscoveryDiscoveredEvent.Finding("PRIVACY_POLICY", "Privacy policy",
                        "https://example.com/privacy", "Publicly linked", "MEDIUM"),
                new PrivacyDiscoveryDiscoveredEvent.Finding("PRIVACY_CONTACT_EMAIL", "privacy@example.com",
                        "mailto:privacy@example.com", "Publicly linked", "MEDIUM")),
                java.time.Instant.now(), version);
    }

    @Test
    void unsupportedEventVersionIsIgnored() {
        service.ingest(event(2, "user-1"));
        verifyNoInteractions(findingRepository, websiteRepository, contactService);
    }

    @Test
    void findingsAreStoredWithEvidenceAndAppliedOnlyToTheOwnedWebsite() {
        UUID websiteId = UUID.randomUUID();
        when(findingRepository.existsByUserIdAndDomainAndFindingTypeAndSourceUrl(
                anyString(), anyString(), anyString(), any())).thenReturn(false);
        when(websiteRepository.findFirstByUserIdAndDomain("user-1", "example.com"))
                .thenReturn(Optional.of(TrackedWebsite.builder().id(websiteId).userId("user-1").build()));
        when(findingRepository.findAllByUserIdAndDomainOrderByCreatedAtAsc("user-1", "example.com"))
                .thenReturn(List.of(
                        PrivacyDiscoveryFinding.builder().findingType("PRIVACY_POLICY")
                                .sourceUrl("https://example.com/privacy").build(),
                        PrivacyDiscoveryFinding.builder().findingType("PRIVACY_CONTACT_EMAIL")
                                .name("privacy@example.com").sourceUrl("mailto:privacy@example.com").build()));

        service.ingest(event(1, "user-1"));

        ArgumentCaptor<PrivacyDiscoveryFinding> saved = ArgumentCaptor.forClass(PrivacyDiscoveryFinding.class);
        verify(findingRepository, times(2)).save(saved.capture());
        assertEquals("example.com", saved.getAllValues().get(0).getDomain());
        assertEquals("https://example.com/privacy", saved.getAllValues().get(0).getSourceUrl());

        ArgumentCaptor<UpdatePrivacyContactRequest> contact = ArgumentCaptor.forClass(UpdatePrivacyContactRequest.class);
        verify(contactService).upsert(eq("user-1"), eq(websiteId), contact.capture());
        assertEquals("https://example.com/privacy", contact.getValue().privacyPolicyUrl());
        assertEquals("privacy@example.com", contact.getValue().privacyContactEmail());
        assertTrue(contact.getValue().sourceUrls().contains("mailto:privacy@example.com"));
        verify(websiteRepository).save(any(TrackedWebsite.class));
    }

    @Test
    void findingsForAnotherUsersDomainAreNeverApplied() {
        when(websiteRepository.findFirstByUserIdAndDomain("user-2", "example.com")).thenReturn(Optional.empty());
        service.ingest(event(1, "user-2"));
        verify(contactService, never()).upsert(anyString(), any(), any());
    }
}
