package com.linklens.userservice.digitalfootprint.service;

import com.linklens.userservice.digitalfootprint.dto.CreatePersonalDataRecordRequest;
import com.linklens.userservice.digitalfootprint.entity.PersonalDataRecord;
import com.linklens.userservice.digitalfootprint.entity.TrackedWebsite;
import com.linklens.userservice.digitalfootprint.exception.InvalidRequestException;
import com.linklens.userservice.digitalfootprint.exception.ResourceNotFoundException;
import com.linklens.userservice.digitalfootprint.repo.PersonalDataRecordRepository;
import com.linklens.userservice.digitalfootprint.repo.TrackedWebsiteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonalDataRecordServiceTest {

    @Mock PersonalDataRecordRepository repository;
    @Mock TrackedWebsiteRepository websiteRepository;
    @InjectMocks PersonalDataRecordService service;

    private static final String USER = "user-1";

    @Test
    void createRequiresWebsiteOwnership() {
        UUID websiteId = UUID.randomUUID();
        when(websiteRepository.findByIdAndUserId(websiteId, USER)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.create(USER, websiteId,
                new CreatePersonalDataRecordRequest(null, "BASIC_CONTACT", "email address", null, "USER_CONFIRMED", null, null)));
        verify(repository, never()).save(any());
    }

    @Test
    void confirmedSourceSetsConfirmedFlagAndVerificationTime() {
        UUID websiteId = UUID.randomUUID();
        when(websiteRepository.findByIdAndUserId(websiteId, USER)).thenReturn(Optional.of(TrackedWebsite.builder().id(websiteId).userId(USER).build()));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PersonalDataRecord record = service.create(USER, websiteId,
                new CreatePersonalDataRecordRequest(null, "BASIC_CONTACT", "email address", null, "USER_CONFIRMED", null, null));

        assertTrue(record.isConfirmedByUser());
        assertFalse(record.isInferredByAi());
        assertNotNull(record.getLastVerifiedAt());
    }

    @Test
    void inferredSourceIsNeverMarkedConfirmed() {
        UUID websiteId = UUID.randomUUID();
        when(websiteRepository.findByIdAndUserId(websiteId, USER)).thenReturn(Optional.of(TrackedWebsite.builder().id(websiteId).userId(USER).build()));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PersonalDataRecord record = service.create(USER, websiteId,
                new CreatePersonalDataRecordRequest(null, "FINANCIAL", "payment method", null, "INFERRED_BY_AI", null, null));

        assertTrue(record.isInferredByAi());
        assertFalse(record.isConfirmedByUser());
        assertNull(record.getLastVerifiedAt());
    }

    @Test
    void rejectsPasswordAndCardSecrets() {
        UUID websiteId = UUID.randomUUID();
        when(websiteRepository.findByIdAndUserId(websiteId, USER)).thenReturn(Optional.of(TrackedWebsite.builder().id(websiteId).userId(USER).build()));

        assertThrows(InvalidRequestException.class, () -> service.create(USER, websiteId,
                new CreatePersonalDataRecordRequest(null, "ACCOUNT", "PASSWORD", null, "USER_CONFIRMED", null, null)));
        assertThrows(InvalidRequestException.class, () -> service.create(USER, websiteId,
                new CreatePersonalDataRecordRequest(null, "FINANCIAL", "card", "4111111111111111", "USER_CONFIRMED", null, null)));
        verify(repository, never()).save(any());
    }

    @Test
    void deleteRejectsAnotherUsersRecord() {
        UUID recordId = UUID.randomUUID();
        when(repository.findByIdAndUserId(recordId, USER)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.delete(USER, recordId));
        verify(repository, never()).delete(any());
    }
}
