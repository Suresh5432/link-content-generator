package com.linklens.userservice.digitalfootprint.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PrivacyRequestStatusTest {

    @Test
    void approvalThenSubmissionIsTheOnlyHappyPathToSubmitted() {
        assertFalse(PrivacyRequestStatus.DRAFT.canTransitionTo(PrivacyRequestStatus.SUBMITTED));
        assertTrue(PrivacyRequestStatus.DRAFT.canTransitionTo(PrivacyRequestStatus.AWAITING_USER_APPROVAL));
        assertTrue(PrivacyRequestStatus.AWAITING_USER_APPROVAL.canTransitionTo(PrivacyRequestStatus.SUBMITTED));
    }

    @Test
    void submittedCanProgressAndTerminate() {
        assertTrue(PrivacyRequestStatus.SUBMITTED.canTransitionTo(PrivacyRequestStatus.ACKNOWLEDGED));
        assertTrue(PrivacyRequestStatus.SUBMITTED.canTransitionTo(PrivacyRequestStatus.IN_PROGRESS));
        assertTrue(PrivacyRequestStatus.SUBMITTED.canTransitionTo(PrivacyRequestStatus.COMPLETED));
        assertFalse(PrivacyRequestStatus.SUBMITTED.canTransitionTo(PrivacyRequestStatus.DRAFT));
    }

    @Test
    void terminalStatusesAreRecognised() {
        assertTrue(PrivacyRequestStatus.COMPLETED.isTerminal());
        assertTrue(PrivacyRequestStatus.CANCELLED.isTerminal());
        assertTrue(PrivacyRequestStatus.REJECTED.isTerminal());
        assertTrue(PrivacyRequestStatus.EXPIRED.isTerminal());
        assertFalse(PrivacyRequestStatus.IN_PROGRESS.isTerminal());
        assertFalse(PrivacyRequestStatus.AWAITING_USER_APPROVAL.isTerminal());
    }

    @Test
    void unknownStatusIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> PrivacyRequestStatus.parse("NOT_A_STATUS"));
        assertEquals(PrivacyRequestStatus.DRAFT, PrivacyRequestStatus.parse(null));
    }
}
