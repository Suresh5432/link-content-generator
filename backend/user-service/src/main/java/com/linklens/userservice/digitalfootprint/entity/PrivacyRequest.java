package com.linklens.userservice.digitalfootprint.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "privacy_requests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PrivacyRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private String userId;
    @Column(name = "tracked_website_id")
    private UUID trackedWebsiteId;
    @Column(name = "request_type", nullable = false)
    private String requestType;
    @Column(nullable = false)
    private String status;
    @Column(name = "request_message", columnDefinition = "TEXT")
    private String requestMessage;
    @Column(name = "external_reference")
    private String externalReference;
    @Column(name = "privacy_contact_url", columnDefinition = "TEXT")
    private String privacyContactUrl;
    @Column(name = "privacy_contact_email")
    private String privacyContactEmail;
    @Column(name = "data_categories", columnDefinition = "TEXT")
    private String dataCategories;
    @Column(name = "preferred_contact")
    private String preferredContact;
    @Column(name = "legal_basis")
    private String legalBasis;
    @Column(name = "submission_method")
    private String submissionMethod;
    @Column(name = "submission_evidence", columnDefinition = "TEXT")
    private String submissionEvidence;
    @Column(name = "expected_response_date")
    private LocalDate expectedResponseDate;
    @Column(name = "submitted_at")
    private Instant submittedAt;
    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;
    @Column(name = "completed_at")
    private Instant completedAt;
    @Column(name = "website_response", columnDefinition = "TEXT")
    private String websiteResponse;
    @Column(name = "user_verified", nullable = false)
    private boolean userVerified;
    @Column(name = "website_claimed_deletion", nullable = false)
    private boolean websiteClaimedDeletion;
    @Column(name = "reminder_at")
    private Instant reminderAt;
    @Column(name = "last_reminder_at")
    private Instant lastReminderAt;
    @Column(name = "approved_at")
    private Instant approvedAt;
    @Column(name = "cancelled_at")
    private Instant cancelledAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @PrePersist
    public void onCreate(){ createdAt=Instant.now(); updatedAt=Instant.now(); if (status == null) status = "DRAFT"; }
    @PreUpdate
    public void onUpdate(){ updatedAt=Instant.now(); }
}
