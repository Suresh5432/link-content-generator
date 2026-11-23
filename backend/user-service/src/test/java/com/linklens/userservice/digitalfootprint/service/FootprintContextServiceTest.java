package com.linklens.userservice.digitalfootprint.service;

import com.linklens.userservice.digitalfootprint.dto.FootprintContextResponse;
import com.linklens.userservice.digitalfootprint.entity.PersonalDataRecord;
import com.linklens.userservice.digitalfootprint.entity.PrivacyRequest;
import com.linklens.userservice.digitalfootprint.entity.TrackedWebsite;
import com.linklens.userservice.digitalfootprint.repo.CookieInventoryRepository;
import com.linklens.userservice.digitalfootprint.repo.PersonalDataRecordRepository;
import com.linklens.userservice.digitalfootprint.repo.PrivacyRequestRepository;
import com.linklens.userservice.digitalfootprint.repo.TrackedWebsiteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FootprintContextServiceTest {

    @Mock TrackedWebsiteRepository websiteRepository;
    @Mock PersonalDataRecordRepository dataRecordRepository;
    @Mock PrivacyRequestRepository requestRepository;
    @Mock CookieInventoryRepository cookieRepository;
    @InjectMocks FootprintContextService service;

    @Test
    void contextContainsOnlyTheRequestedUsersRecordsWithEvidenceLabels() {
        UUID websiteId = UUID.randomUUID();
        when(websiteRepository.findAllByUserIdOrderByCreatedAtDesc("user-1")).thenReturn(List.of(
                TrackedWebsite.builder().id(websiteId).userId("user-1").name("Example Bank")
                        .domain("example.com").riskLevel("HIGH").build()));
        when(dataRecordRepository.findAllByUserIdOrderByCreatedAtDesc("user-1")).thenReturn(List.of(
                PersonalDataRecord.builder().trackedWebsiteId(websiteId).dataCategory("BASIC")
                        .dataType("EMAIL").confirmedByUser(true).source("USER_CONFIRMED").build(),
                PersonalDataRecord.builder().trackedWebsiteId(websiteId).dataCategory("FINANCIAL")
                        .dataType("PAYMENT_INFO").inferredByAi(true).source("INFERRED_BY_AI").build()));
        when(requestRepository.findAllByUserIdOrderByCreatedAtDesc("user-1")).thenReturn(List.of(
                PrivacyRequest.builder().trackedWebsiteId(websiteId).requestType("DELETION")
                        .status("SUBMITTED").websiteClaimedDeletion(true).build()));
        when(cookieRepository.findDistinctDomains("user-1")).thenReturn(List.of("example.com"));
        when(cookieRepository.countByUserIdAndTrackingRelatedTrue("user-1")).thenReturn(3L);

        FootprintContextResponse context = service.buildContext("user-1");

        assertEquals(1, context.websites().size());
        assertEquals("Example Bank", context.websites().get(0).name());
        assertEquals(2, context.dataRecords().size());
        assertEquals("USER_CONFIRMED", context.dataRecords().get(0).sourceLabel());
        assertEquals("INFERRED_BY_AI", context.dataRecords().get(1).sourceLabel());
        assertEquals("Example Bank", context.dataRecords().get(1).website());
        assertEquals(1, context.requests().size());
        assertTrue(context.requests().get(0).websiteClaimedDeletion());
        assertFalse(context.requests().get(0).userVerified());
        assertEquals(3L, context.trackingCookieCount());
        verify(websiteRepository).findAllByUserIdOrderByCreatedAtDesc("user-1");
        verify(dataRecordRepository).findAllByUserIdOrderByCreatedAtDesc("user-1");
        verify(requestRepository).findAllByUserIdOrderByCreatedAtDesc("user-1");
        verify(cookieRepository).findDistinctDomains("user-1");
    }
}
