package com.linklens.userservice.digitalfootprint.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "connected_email_accounts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ConnectedEmailAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private String userId;
    @Column(nullable = false)
    private String provider;
    @Column(name = "account_email")
    private String accountEmail;
    @Column(columnDefinition = "TEXT")
    private String scopes;
    @Column(nullable = false)
    private String status;
    @Column(name = "consented_at")
    private Instant consentedAt;
    @Column(name = "last_scan_at")
    private Instant lastScanAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist public void onCreate() { createdAt = Instant.now(); updatedAt = Instant.now(); if (status == null) status = "DISABLED"; }
    @PreUpdate public void onUpdate() { updatedAt = Instant.now(); }
}
