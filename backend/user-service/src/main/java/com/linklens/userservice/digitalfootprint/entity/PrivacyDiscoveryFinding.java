package com.linklens.userservice.digitalfootprint.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "privacy_discovery_findings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PrivacyDiscoveryFinding {
    @Id
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private String userId;
    @Column(nullable = false)
    private String domain;
    @Column(name = "finding_type", nullable = false, length = 60)
    private String findingType;
    private String name;
    @Column(name = "source_url", columnDefinition = "TEXT")
    private String sourceUrl;
    @Column(columnDefinition = "TEXT")
    private String description;
    private String confidence;
    @Column(name = "analysis_id")
    private UUID analysisId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }
}
