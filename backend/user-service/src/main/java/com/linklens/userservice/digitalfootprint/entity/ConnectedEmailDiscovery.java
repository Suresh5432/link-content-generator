package com.linklens.userservice.digitalfootprint.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "connected_email_discoveries")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ConnectedEmailDiscovery {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "connected_account_id", nullable = false)
    private UUID connectedAccountId;
    @Column(name = "user_id", nullable = false)
    private String userId;
    @Column(nullable = false)
    private String provider;
    @Column(nullable = false)
    private String domain;
    @Column(name = "evidence_type", nullable = false)
    private String evidenceType;
    @Column(name = "evidence_source", columnDefinition = "TEXT")
    private String evidenceSource;
    @Column(name = "first_seen_at")
    private Instant firstSeenAt;
    @Column(name = "last_seen_at")
    private Instant lastSeenAt;
    private String confidence;
    @Column(name = "confirmed_by_user", nullable = false)
    private boolean confirmedByUser;
    @Column(name = "dismissed_by_user", nullable = false)
    private boolean dismissedByUser;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist public void onCreate() { createdAt = Instant.now(); updatedAt = Instant.now(); }
    @PreUpdate public void onUpdate() { updatedAt = Instant.now(); }
}
