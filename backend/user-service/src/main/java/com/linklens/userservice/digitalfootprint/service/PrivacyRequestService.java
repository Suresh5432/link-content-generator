package com.linklens.userservice.digitalfootprint.service;

import com.linklens.userservice.digitalfootprint.domain.EvidenceType;
import com.linklens.userservice.digitalfootprint.domain.PrivacyRequestStatus;
import com.linklens.userservice.digitalfootprint.domain.PrivacyRequestType;
import com.linklens.userservice.digitalfootprint.dto.CreateEvidenceRequest;
import com.linklens.userservice.digitalfootprint.dto.CreatePrivacyRequest;
import com.linklens.userservice.digitalfootprint.dto.UpdatePrivacyRequestRequest;
import com.linklens.userservice.digitalfootprint.entity.PrivacyRequest;
import com.linklens.userservice.digitalfootprint.entity.PrivacyRequestEvidence;
import com.linklens.userservice.digitalfootprint.entity.PrivacyRequestHistory;
import com.linklens.userservice.digitalfootprint.event.PrivacyRequestChangedEvent;
import com.linklens.userservice.digitalfootprint.exception.IllegalStateTransitionException;
import com.linklens.userservice.digitalfootprint.exception.InvalidRequestException;
import com.linklens.userservice.digitalfootprint.exception.ResourceNotFoundException;
import com.linklens.userservice.digitalfootprint.repo.PrivacyRequestEvidenceRepository;
import com.linklens.userservice.digitalfootprint.repo.PrivacyRequestHistoryRepository;
import com.linklens.userservice.digitalfootprint.repo.PrivacyRequestRepository;
import com.linklens.userservice.digitalfootprint.repo.TrackedWebsiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class PrivacyRequestService {

    private static final List<PrivacyRequestStatus> NON_TERMINAL = Stream.of(PrivacyRequestStatus.values())
            .filter(s -> !s.isTerminal())
            .toList();
    private static final List<String> PENDING_REMINDER_STATUSES = List.of(
            PrivacyRequestStatus.SUBMITTED.name(),
            PrivacyRequestStatus.ACKNOWLEDGED.name(),
            PrivacyRequestStatus.IN_PROGRESS.name(),
            PrivacyRequestStatus.NEEDS_VERIFICATION.name());

    private final PrivacyRequestRepository repository;
    private final PrivacyRequestHistoryRepository historyRepository;
    private final PrivacyRequestEvidenceRepository evidenceRepository;
    private final TrackedWebsiteRepository websiteRepository;
    private final ApplicationEventPublisher events;

    @Transactional
    public PrivacyRequest create(String userId, CreatePrivacyRequest request) {
        PrivacyRequestType type = PrivacyRequestType.parse(request.requestType());
        UUID websiteId = Ids.parseNullableUuid(request.trackedWebsiteId(), "trackedWebsiteId");
        if (websiteId != null) {
            websiteRepository.findByIdAndUserId(websiteId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Tracked website not found"));
            List<String> blocking = NON_TERMINAL.stream().map(Enum::name).toList();
            if (repository.existsByUserIdAndTrackedWebsiteIdAndRequestTypeAndStatusIn(userId, websiteId, type.name(), blocking)) {
                throw new InvalidRequestException("An open " + type.name() + " request already exists for this website");
            }
        }
        String message = trimToNull(request.requestMessage());
        if (message == null) message = PrivacyRequestMessageFactory.build(type, trimToNull(request.dataCategories()));
        SensitiveDataGuard.rejectSecretMaterial(request.requestMessage());
        SensitiveDataGuard.rejectSecretMaterial(request.privacyContactUrl());
        SensitiveDataGuard.rejectSecretMaterial(request.privacyContactEmail());
        SensitiveDataGuard.rejectSecretMaterial(request.dataCategories());
        SensitiveDataGuard.rejectSecretMaterial(request.preferredContact());
        SensitiveDataGuard.rejectSecretMaterial(request.legalBasis());
        PrivacyRequest r = PrivacyRequest.builder()
                .userId(userId)
                .trackedWebsiteId(websiteId)
                .requestType(type.name())
                .status(PrivacyRequestStatus.DRAFT.name())
                .requestMessage(message)
                .privacyContactUrl(trimToNull(request.privacyContactUrl()))
                .privacyContactEmail(trimToNull(request.privacyContactEmail()))
                .dataCategories(trimToNull(request.dataCategories()))
                .preferredContact(trimToNull(request.preferredContact()))
                .legalBasis(trimToNull(request.legalBasis()))
                .build();
        PrivacyRequest saved = repository.save(r);
        history(saved, null, saved.getStatus(), "Request prepared");
        events.publishEvent(new PrivacyRequestChangedEvent(saved.getId(), userId, saved.getRequestType(), null, saved.getStatus()));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<PrivacyRequest> list(String userId) {
        return repository.findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public PrivacyRequest get(String userId, UUID id) {
        return requireOwned(userId, id);
    }

    @Transactional
    public PrivacyRequest update(String userId, UUID id, UpdatePrivacyRequestRequest request) {
        PrivacyRequest r = requireOwned(userId, id);
        if (isTerminal(r)) throw new IllegalStateTransitionException("Completed, rejected, expired or cancelled requests cannot be edited");
        SensitiveDataGuard.rejectSecretMaterial(request.requestMessage());
        SensitiveDataGuard.rejectSecretMaterial(request.privacyContactUrl());
        SensitiveDataGuard.rejectSecretMaterial(request.privacyContactEmail());
        SensitiveDataGuard.rejectSecretMaterial(request.dataCategories());
        SensitiveDataGuard.rejectSecretMaterial(request.preferredContact());
        SensitiveDataGuard.rejectSecretMaterial(request.legalBasis());
        if (request.submissionMethod() != null || request.submissionEvidence() != null) {
            throw new InvalidRequestException("Submission details are recorded by the submit endpoint or as evidence");
        }
        if (request.requestMessage() != null) r.setRequestMessage(trimToNull(request.requestMessage()));
        if (request.privacyContactUrl() != null) r.setPrivacyContactUrl(trimToNull(request.privacyContactUrl()));
        if (request.privacyContactEmail() != null) r.setPrivacyContactEmail(trimToNull(request.privacyContactEmail()));
        if (request.dataCategories() != null) r.setDataCategories(trimToNull(request.dataCategories()));
        if (request.preferredContact() != null) r.setPreferredContact(trimToNull(request.preferredContact()));
        if (request.legalBasis() != null) r.setLegalBasis(trimToNull(request.legalBasis()));
        if (request.expectedResponseDate() != null) r.setExpectedResponseDate(Ids.parseDate(request.expectedResponseDate(), "expectedResponseDate"));
        if (r.getStatus().equals(PrivacyRequestStatus.DRAFT.name()) || r.getStatus().equals(PrivacyRequestStatus.READY_FOR_REVIEW.name())) {
            transition(r, PrivacyRequestStatus.READY_FOR_REVIEW, "Request edited and ready for review");
        }
        return repository.save(r);
    }

    @Transactional
    public PrivacyRequest requestReview(String userId, UUID id) {
        PrivacyRequest r = requireOwned(userId, id);
        if (r.getStatus().equals(PrivacyRequestStatus.READY_FOR_REVIEW.name())) return r;
        transition(r, PrivacyRequestStatus.READY_FOR_REVIEW, "Marked ready for review");
        return repository.save(r);
    }

    @Transactional
    public PrivacyRequest approve(String userId, UUID id) {
        PrivacyRequest r = requireOwned(userId, id);
        if (r.getStatus().equals(PrivacyRequestStatus.AWAITING_USER_APPROVAL.name()) && r.getApprovedAt() != null) {
            return r;
        }
        if (r.getStatus().equals(PrivacyRequestStatus.SUBMITTED.name()) || r.getStatus().equals(PrivacyRequestStatus.ACKNOWLEDGED.name())
                || r.getStatus().equals(PrivacyRequestStatus.IN_PROGRESS.name())) {
            throw new IllegalStateTransitionException("Request has already been submitted");
        }
        if (isTerminal(r)) throw new IllegalStateTransitionException("Cannot approve a " + r.getStatus() + " request");
        if (r.getRequestMessage() == null || r.getRequestMessage().isBlank()) {
            throw new InvalidRequestException("A request message is required before approval");
        }
        r.setApprovedAt(Instant.now());
        transition(r, PrivacyRequestStatus.AWAITING_USER_APPROVAL, "User approved the exact request message");
        return repository.save(r);
    }

    @Transactional
    public PrivacyRequest submit(String userId, UUID id, String submissionMethod, String externalReference) {
        PrivacyRequest r = requireOwned(userId, id);
        if (r.getApprovedAt() == null) {
            throw new IllegalStateTransitionException("The request must be approved by the user before submission");
        }
        if (!r.getStatus().equals(PrivacyRequestStatus.AWAITING_USER_APPROVAL.name())) {
            throw new IllegalStateTransitionException("Only an approved request awaiting submission can be submitted");
        }
        r.setSubmittedAt(Instant.now());
        r.setSubmissionMethod(trimToNull(submissionMethod) == null ? "MANUAL" : submissionMethod.trim().toUpperCase(Locale.ROOT));
        if (externalReference != null) r.setExternalReference(trimToNull(externalReference));
        if (r.getExpectedResponseDate() == null) r.setExpectedResponseDate(r.getSubmittedAt().atZone(java.time.ZoneOffset.UTC).toLocalDate().plusDays(30));
        r.setReminderAt(r.getSubmittedAt().plus(14, ChronoUnit.DAYS));
        transition(r, PrivacyRequestStatus.SUBMITTED, "Request submitted by user");
        return repository.save(r);
    }

    @Transactional
    public PrivacyRequest cancel(String userId, UUID id) {
        PrivacyRequest r = requireOwned(userId, id);
        if (isTerminal(r) && r.getStatus().equals(PrivacyRequestStatus.CANCELLED.name())) return r;
        if (r.getStatus().equals(PrivacyRequestStatus.COMPLETED.name())) {
            throw new IllegalStateTransitionException("A completed request cannot be cancelled");
        }
        r.setCancelledAt(Instant.now());
        transition(r, PrivacyRequestStatus.CANCELLED, "Request cancelled by user");
        return repository.save(r);
    }

    @Transactional
    public PrivacyRequest updateStatus(String userId, UUID id, String statusValue, String note) {
        PrivacyRequest r = requireOwned(userId, id);
        PrivacyRequestStatus target;
        try {
            target = PrivacyRequestStatus.parse(statusValue);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Unknown request status: " + statusValue);
        }
        if (target == PrivacyRequestStatus.SUBMITTED) {
            throw new IllegalStateTransitionException("Use the submit endpoint after the user approves the exact message");
        }
        if (target == PrivacyRequestStatus.COMPLETED) {
            throw new IllegalStateTransitionException("Completion requires independent user verification");
        }
        if (target == PrivacyRequestStatus.AWAITING_USER_APPROVAL || target == PrivacyRequestStatus.READY_FOR_REVIEW) {
            throw new IllegalStateTransitionException("Use the prepare and approval endpoints for review states");
        }
        SensitiveDataGuard.rejectSecretMaterial(note);
        transition(r, target, note == null || note.isBlank() ? "Status updated by user" : note.trim());
        if (target == PrivacyRequestStatus.ACKNOWLEDGED) r.setAcknowledgedAt(Instant.now());
        if (target == PrivacyRequestStatus.COMPLETED) r.setCompletedAt(Instant.now());
        if (target == PrivacyRequestStatus.CANCELLED) r.setCancelledAt(Instant.now());
        return repository.save(r);
    }

    @Transactional
    public PrivacyRequest recordResponse(String userId, UUID id, String response, boolean websiteClaimedDeletion) {
        PrivacyRequest r = requireOwned(userId, id);
        SensitiveDataGuard.rejectSecretMaterial(response);
        r.setWebsiteResponse(trimToNull(response));
        r.setWebsiteClaimedDeletion(websiteClaimedDeletion);
        if (r.getStatus().equals(PrivacyRequestStatus.SUBMITTED.name())) {
            transition(r, PrivacyRequestStatus.ACKNOWLEDGED, "Website response recorded");
            r.setAcknowledgedAt(Instant.now());
        }
        return repository.save(r);
    }

    @Transactional
    public PrivacyRequest verifyCompletion(String userId, UUID id, boolean userVerified) {
        PrivacyRequest r = requireOwned(userId, id);
        r.setUserVerified(userVerified);
        if (userVerified) {
            if (!r.getStatus().equals(PrivacyRequestStatus.NEEDS_VERIFICATION.name())) {
                transition(r, PrivacyRequestStatus.NEEDS_VERIFICATION, "Verification requested before marking completed");
            }
            transition(r, PrivacyRequestStatus.COMPLETED, "User independently verified completion");
            r.setCompletedAt(Instant.now());
        } else if (r.getStatus().equals(PrivacyRequestStatus.COMPLETED.name())) {
            transition(r, PrivacyRequestStatus.NEEDS_VERIFICATION, "User reopened the request for verification");
        }
        return repository.save(r);
    }

    @Transactional
    public PrivacyRequestEvidence addEvidence(String userId, UUID id, CreateEvidenceRequest request) {
        PrivacyRequest r = requireOwned(userId, id);
        SensitiveDataGuard.rejectSecretMaterial(request.description());
        PrivacyRequestEvidence evidence = PrivacyRequestEvidence.builder()
                .requestId(r.getId())
                .userId(userId)
                .type(EvidenceType.parse(request.type()).name())
                .description(trimToNull(request.description()))
                .referenceUrl(trimToNull(request.referenceUrl()))
                .build();
        return evidenceRepository.save(evidence);
    }

    @Transactional(readOnly = true)
    public List<PrivacyRequestEvidence> evidence(String userId, UUID id) {
        requireOwned(userId, id);
        return evidenceRepository.findAllByRequestIdAndUserIdOrderByCreatedAtAsc(id, userId);
    }

    @Transactional(readOnly = true)
    public List<PrivacyRequestHistory> history(String userId, UUID id) {
        requireOwned(userId, id);
        return historyRepository.findAllByRequestIdAndUserIdOrderByCreatedAtAsc(id, userId);
    }

    @Transactional(readOnly = true)
    public List<PrivacyRequest> dueReminders(String userId) {
        Instant now = Instant.now();
        return repository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(r -> PENDING_REMINDER_STATUSES.contains(r.getStatus()))
                .filter(r -> r.getReminderAt() != null && !r.getReminderAt().isAfter(now))
                .toList();
    }

    @Transactional
    public PrivacyRequest markReminderSent(String userId, UUID id) {
        PrivacyRequest r = requireOwned(userId, id);
        r.setLastReminderAt(Instant.now());
        r.setReminderAt(Instant.now().plus(14, ChronoUnit.DAYS));
        return repository.save(r);
    }

    @Transactional(readOnly = true)
    public PrivacyRequest requireOwned(String userId, UUID id) {
        return repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Privacy request not found"));
    }

    private void transition(PrivacyRequest r, PrivacyRequestStatus target, String note) {
        PrivacyRequestStatus current = PrivacyRequestStatus.parse(r.getStatus());
        if (current == target) return;
        if (!current.canTransitionTo(target)) {
            throw new IllegalStateTransitionException("Cannot move request from " + current + " to " + target);
        }
        r.setStatus(target.name());
        history(r, current.name(), target.name(), note);
        events.publishEvent(new PrivacyRequestChangedEvent(r.getId(), r.getUserId(), r.getRequestType(), current.name(), target.name()));
    }

    private void history(PrivacyRequest r, String from, String to, String note) {
        historyRepository.save(PrivacyRequestHistory.builder()
                .requestId(r.getId())
                .userId(r.getUserId())
                .fromStatus(from)
                .toStatus(to)
                .note(note)
                .build());
    }

    private static boolean isTerminal(PrivacyRequest r) {
        return PrivacyRequestStatus.parse(r.getStatus()).isTerminal();
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
