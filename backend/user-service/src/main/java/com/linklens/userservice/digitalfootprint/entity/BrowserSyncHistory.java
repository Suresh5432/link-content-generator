package com.linklens.userservice.digitalfootprint.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "browser_sync_history")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BrowserSyncHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "integration_id", nullable = false)
    private UUID integrationId;
    @Column(name = "user_id", nullable = false)
    private String userId;
    @Column(name = "cookie_count", nullable = false)
    private int cookieCount;
    @Column(name = "domains_count", nullable = false)
    private int domainsCount;
    @Column(nullable = false)
    private String status;
    @Column(columnDefinition = "TEXT")
    private String message;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @PrePersist
    public void onCreate(){ createdAt=Instant.now(); }
}
