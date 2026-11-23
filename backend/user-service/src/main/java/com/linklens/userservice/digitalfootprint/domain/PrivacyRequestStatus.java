package com.linklens.userservice.digitalfootprint.domain;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum PrivacyRequestStatus {
    DRAFT,
    READY_FOR_REVIEW,
    AWAITING_USER_APPROVAL,
    SUBMITTED,
    ACKNOWLEDGED,
    IN_PROGRESS,
    NEEDS_VERIFICATION,
    COMPLETED,
    REJECTED,
    EXPIRED,
    FAILED,
    CANCELLED;

    private static final Map<PrivacyRequestStatus, Set<PrivacyRequestStatus>> ALLOWED = Map.ofEntries(
            Map.entry(DRAFT, EnumSet.of(READY_FOR_REVIEW, AWAITING_USER_APPROVAL, CANCELLED)),
            Map.entry(READY_FOR_REVIEW, EnumSet.of(AWAITING_USER_APPROVAL, DRAFT, CANCELLED)),
            Map.entry(AWAITING_USER_APPROVAL, EnumSet.of(SUBMITTED, READY_FOR_REVIEW, CANCELLED)),
            Map.entry(SUBMITTED, EnumSet.of(ACKNOWLEDGED, IN_PROGRESS, NEEDS_VERIFICATION, REJECTED, EXPIRED, FAILED, CANCELLED)),
            Map.entry(ACKNOWLEDGED, EnumSet.of(IN_PROGRESS, NEEDS_VERIFICATION, REJECTED, EXPIRED, FAILED)),
            Map.entry(IN_PROGRESS, EnumSet.of(NEEDS_VERIFICATION, REJECTED, EXPIRED, FAILED)),
            Map.entry(NEEDS_VERIFICATION, EnumSet.of(IN_PROGRESS, COMPLETED, REJECTED, EXPIRED, FAILED)),
            Map.entry(COMPLETED, EnumSet.of(NEEDS_VERIFICATION)),
            Map.entry(REJECTED, EnumSet.of(IN_PROGRESS, CANCELLED)),
            Map.entry(EXPIRED, EnumSet.of(IN_PROGRESS, CANCELLED)),
            Map.entry(FAILED, EnumSet.of(IN_PROGRESS, CANCELLED)),
            Map.entry(CANCELLED, EnumSet.of(DRAFT))
    );

    public boolean canTransitionTo(PrivacyRequestStatus target) {
        return target != null && ALLOWED.getOrDefault(this, Set.of()).contains(target);
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == REJECTED || this == EXPIRED || this == CANCELLED;
    }

    public static PrivacyRequestStatus parse(String value) {
        if (value == null || value.isBlank()) return DRAFT;
        return valueOf(value.trim().toUpperCase());
    }
}
