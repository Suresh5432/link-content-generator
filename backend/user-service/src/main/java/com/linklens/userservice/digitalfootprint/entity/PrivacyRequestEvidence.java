package com.linklens.userservice.digitalfootprint.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "privacy_request_evidence")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PrivacyRequestEvidence {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "request_id", nullable = false)
    private UUID requestId;
    @Column(name = "user_id", nullable = false)
    private String userId;
    @Column(nullable = false)
    private String type;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(name = "reference_url", columnDefinition = "TEXT")
    private String referenceUrl;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @PrePersist
    public void onCreate(){ createdAt=Instant.now(); }
}
