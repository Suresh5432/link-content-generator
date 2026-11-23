package com.linklens.userservice.digitalfootprint.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "user_website_accounts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserWebsiteAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "tracked_website_id", nullable = false)
    private UUID trackedWebsiteId;
    @Column(name = "user_id", nullable = false)
    private String userId;
    @Column(name = "account_identifier")
    private String accountIdentifier;
    @Column(name = "account_creation_date")
    private LocalDate accountCreationDate;
    @Column(name = "account_category")
    private String accountCategory;
    @Column(name = "shared_information", columnDefinition = "TEXT")
    private String sharedInformation;
    @Column(name = "account_status")
    private String accountStatus;
    @Column(name = "discovery_source", nullable = false)
    private String discoverySource;
    @Column(nullable = false)
    private boolean confirmed;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @PrePersist
    public void onCreate(){ createdAt=Instant.now(); updatedAt=Instant.now(); }
    @PreUpdate
    public void onUpdate(){ updatedAt=Instant.now(); }
}
