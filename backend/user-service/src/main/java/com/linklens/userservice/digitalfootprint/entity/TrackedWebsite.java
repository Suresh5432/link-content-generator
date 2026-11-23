package com.linklens.userservice.digitalfootprint.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tracked_websites")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrackedWebsite {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private String userId;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String domain;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;
    private String category;
    @Column(name = "account_status")
    private String accountStatus;
    @Column(name = "privacy_policy_url", columnDefinition = "TEXT")
    private String privacyPolicyUrl;
    @Column(name = "deletion_policy_url", columnDefinition = "TEXT")
    private String deletionPolicyUrl;
    @Column(name = "risk_level")
    private String riskLevel;
    @Column(name = "last_inspection_at")
    private Instant lastInspectionAt;
    @Column(name = "data_categories", columnDefinition = "TEXT")
    private String dataCategories;
    @Column(name = "tracking_technologies", columnDefinition = "TEXT")
    private String trackingTechnologies;
    @Column(name = "deletion_request_status")
    private String deletionRequestStatus;
    @Column(name = "from_analysis_id")
    private UUID fromAnalysisId;
    @Column(name = "first_recorded_at")
    private Instant firstRecordedAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @PrePersist
    public void onCreate(){ createdAt=Instant.now(); updatedAt=Instant.now(); }
    @PreUpdate
    public void onUpdate(){ updatedAt=Instant.now(); }
}
