package com.linklens.userservice.digitalfootprint.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "personal_data_records")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PersonalDataRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "tracked_website_id", nullable = false)
    private UUID trackedWebsiteId;
    @Column(name = "user_id", nullable = false)
    private String userId;
    @Column(name = "data_category", nullable = false)
    private String dataCategory;
    @Column(name = "data_type", nullable = false)
    private String dataType;
    private String description;
    @Column(nullable = false)
    private String source;
    @Column(name = "confirmed_by_user", nullable = false)
    private boolean confirmedByUser;
    @Column(name = "observed_on_website", nullable = false)
    private boolean observedOnWebsite;
    @Column(name = "declared_in_policy", nullable = false)
    private boolean declaredInPolicy;
    @Column(name = "inferred_by_ai", nullable = false)
    private boolean inferredByAi;
    @Column(name = "date_shared")
    private LocalDate dateShared;
    @Column(name = "evidence_source")
    private String evidenceSource;
    @Column(name = "last_verified_at")
    private Instant lastVerifiedAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @PrePersist
    public void onCreate(){ createdAt=Instant.now(); updatedAt=Instant.now(); }
    @PreUpdate
    public void onUpdate(){ updatedAt=Instant.now(); }
}
