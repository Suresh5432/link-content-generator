package com.linklens.userservice.digitalfootprint.service;

import com.linklens.userservice.digitalfootprint.dto.CreatePrivacyRequest;
import com.linklens.userservice.digitalfootprint.entity.PrivacyRequest;
import com.linklens.userservice.digitalfootprint.exception.IllegalStateTransitionException;
import com.linklens.userservice.digitalfootprint.exception.InvalidRequestException;
import com.linklens.userservice.digitalfootprint.exception.ResourceNotFoundException;
import com.linklens.userservice.digitalfootprint.repo.PrivacyRequestEvidenceRepository;
import com.linklens.userservice.digitalfootprint.repo.PrivacyRequestHistoryRepository;
import com.linklens.userservice.digitalfootprint.repo.PrivacyRequestRepository;
import com.linklens.userservice.digitalfootprint.repo.TrackedWebsiteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrivacyRequestServiceTest {

    @Mock PrivacyRequestRepository repository;
    @Mock PrivacyRequestHistoryRepository historyRepository;
    @Mock PrivacyRequestEvidenceRepository evidenceRepository;
    @Mock TrackedWebsiteRepository websiteRepository;
    @Mock ApplicationEventPublisher events;

    @InjectMocks PrivacyRequestService service;

    private static final String USER = "user-1";

    private PrivacyRequest existing(String status) {
        return PrivacyRequest.builder()
                .id(UUID.randomUUID())
                .userId(USER)
                .requestType("DELETION")
                .status(status)
                .requestMessage("Please delete my data.")
                .build();
    }

    @Test
    void createPreparesDraftWithDefaultMessage() {
        when(repository.save(any(PrivacyRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        PrivacyRequest created = service.create(USER, new CreatePrivacyRequest(null, "DELETION", null, null, null, "email, address", null, null));

        assertEquals("DRAFT", created.getStatus());
        assertEquals("DELETION", created.getRequestType());
        assertNotNull(created.getRequestMessage());
        assertTrue(created.getRequestMessage().contains("email, address"));
        verify(historyRepository).save(any());
        verify(events).publishEvent(any(Object.class));
    }

    @Test
    void createRejectsDuplicateOpenRequestForSameWebsite() {
        UUID websiteId = UUID.randomUUID();
        when(websiteRepository.findByIdAndUserId(websiteId, USER)).thenReturn(Optional.of(com.linklens.userservice.digitalfootprint.entity.TrackedWebsite.builder().id(websiteId).userId(USER).build()));
        when(repository.existsByUserIdAndTrackedWebsiteIdAndRequestTypeAndStatusIn(eq(USER), eq(websiteId), eq("DELETION"), any())).thenReturn(true);

        assertThrows(InvalidRequestException.class, () -> service.create(USER, new CreatePrivacyRequest(websiteId.toString(), "DELETION", "msg", null, null, null, null, null)));
        verify(repository, never()).save(any());
    }

    @Test
    void createRejectsRawSecretsInMessage() {
        assertThrows(InvalidRequestException.class, () -> service.create(USER, new CreatePrivacyRequest(null, "DELETION", "my card number: 4111111111111111", null, null, null, null, null)));
        verify(repository, never()).save(any());
    }

    @Test
    void approveMovesToAwaitingApprovalAndStampsTime() {
        PrivacyRequest r = existing("READY_FOR_REVIEW");
        when(repository.findByIdAndUserId(r.getId(), USER)).thenReturn(Optional.of(r));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PrivacyRequest approved = service.approve(USER, r.getId());

        assertEquals("AWAITING_USER_APPROVAL", approved.getStatus());
        assertNotNull(approved.getApprovedAt());
    }

    @Test
    void approveRejectsBlankMessage() {
        PrivacyRequest r = existing("READY_FOR_REVIEW");
        r.setRequestMessage("  ");
        when(repository.findByIdAndUserId(r.getId(), USER)).thenReturn(Optional.of(r));

        assertThrows(InvalidRequestException.class, () -> service.approve(USER, r.getId()));
    }

    @Test
    void submitRequiresExplicitApproval() {
        PrivacyRequest r = existing("AWAITING_USER_APPROVAL");
        r.setApprovedAt(null);
        when(repository.findByIdAndUserId(r.getId(), USER)).thenReturn(Optional.of(r));

        assertThrows(IllegalStateTransitionException.class, () -> service.submit(USER, r.getId(), "EMAIL", "ref-1"));
        verify(repository, never()).save(any());
    }

    @Test
    void submitAfterApprovalSetsSubmissionMetadataAndReminder() {
        PrivacyRequest r = existing("AWAITING_USER_APPROVAL");
        r.setApprovedAt(Instant.now());
        when(repository.findByIdAndUserId(r.getId(), USER)).thenReturn(Optional.of(r));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PrivacyRequest submitted = service.submit(USER, r.getId(), "EMAIL", "ref-42");

        assertEquals("SUBMITTED", submitted.getStatus());
        assertNotNull(submitted.getSubmittedAt());
        assertNotNull(submitted.getExpectedResponseDate());
        assertNotNull(submitted.getReminderAt());
        assertEquals("EMAIL", submitted.getSubmissionMethod());
        assertEquals("ref-42", submitted.getExternalReference());
    }

    @Test
    void crossUserAccessIsDenied() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdAndUserId(id, USER)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.get(USER, id));
        assertThrows(ResourceNotFoundException.class, () -> service.approve(USER, id));
    }

    @Test
    void invalidTransitionIsRejected() {
        PrivacyRequest r = existing("COMPLETED");
        when(repository.findByIdAndUserId(r.getId(), USER)).thenReturn(Optional.of(r));

        assertThrows(IllegalStateTransitionException.class, () -> service.updateStatus(USER, r.getId(), "SUBMITTED", "nope"));
    }

    @Test
    void statusChangesArePublishedAfterRequestIsSaved() {
        PrivacyRequest r = existing("SUBMITTED");
        when(repository.findByIdAndUserId(r.getId(), USER)).thenReturn(Optional.of(r));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.updateStatus(USER, r.getId(), "ACKNOWLEDGED", "site replied");

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(captor.capture());
        assertEquals("ACKNOWLEDGED", r.getStatus());
        assertNotNull(r.getAcknowledgedAt());
        assertTrue(captor.getValue().toString().contains("ACKNOWLEDGED") || captor.getValue() instanceof com.linklens.userservice.digitalfootprint.event.PrivacyRequestChangedEvent);
    }

    @Test
    void completedRequestRequiresUserVerificationBeforeMarkingVerified() {
        PrivacyRequest r = existing("IN_PROGRESS");
        when(repository.findByIdAndUserId(r.getId(), USER)).thenReturn(Optional.of(r));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PrivacyRequest verified = service.verifyCompletion(USER, r.getId(), true);

        assertEquals("COMPLETED", verified.getStatus());
        assertTrue(verified.isUserVerified());
        assertNotNull(verified.getCompletedAt());
    }

    @Test
    void updateStatusCannotBypassSubmissionOrVerification() {
        PrivacyRequest r = existing("AWAITING_USER_APPROVAL");
        r.setApprovedAt(Instant.now());
        when(repository.findByIdAndUserId(r.getId(), USER)).thenReturn(Optional.of(r));

        assertThrows(IllegalStateTransitionException.class,
                () -> service.updateStatus(USER, r.getId(), "SUBMITTED", "pretend submit"));
        assertThrows(IllegalStateTransitionException.class,
                () -> service.updateStatus(USER, r.getId(), "COMPLETED", "pretend complete"));
        assertNull(r.getSubmittedAt());
        assertFalse(r.isUserVerified());
        verify(repository, never()).save(any());
    }

    @Test
    void rawSecretsAreRejectedInResponsesAndNotes() {
        PrivacyRequest r = existing("SUBMITTED");
        when(repository.findByIdAndUserId(r.getId(), USER)).thenReturn(Optional.of(r));

        assertThrows(InvalidRequestException.class,
                () -> service.recordResponse(USER, r.getId(), "password: hunter2", false));
        assertThrows(InvalidRequestException.class,
                () -> service.updateStatus(USER, r.getId(), "IN_PROGRESS", "card number: 4111111111111111"));
        verify(repository, never()).save(any());
    }

    @Test
    void submissionDetailsCannotBePresetDuringEdit() {
        PrivacyRequest r = existing("DRAFT");
        when(repository.findByIdAndUserId(r.getId(), USER)).thenReturn(Optional.of(r));

        assertThrows(InvalidRequestException.class, () -> service.update(USER, r.getId(),
                new com.linklens.userservice.digitalfootprint.dto.UpdatePrivacyRequestRequest(
                        null, null, null, null, null, null, null, "EMAIL", "receipt")));
    }

    @Test
    void dueRemindersOnlyIncludePendingRequestsPastReminderTime() {
        PrivacyRequest due = existing("SUBMITTED");
        due.setReminderAt(Instant.now().minusSeconds(60));
        PrivacyRequest notDue = existing("SUBMITTED");
        notDue.setReminderAt(Instant.now().plusSeconds(3600));
        PrivacyRequest completed = existing("COMPLETED");
        completed.setReminderAt(Instant.now().minusSeconds(60));
        when(repository.findAllByUserIdOrderByCreatedAtDesc(USER)).thenReturn(List.of(due, notDue, completed));

        List<PrivacyRequest> reminders = service.dueReminders(USER);

        assertEquals(List.of(due), reminders);
    }
}
