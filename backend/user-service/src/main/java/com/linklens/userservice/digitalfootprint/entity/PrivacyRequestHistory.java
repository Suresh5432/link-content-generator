package com.linklens.userservice.digitalfootprint.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "privacy_request_history")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PrivacyRequestHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "request_id", nullable = false)
    private UUID requestId;
    @Column(name = "user_id", nullable = false)
    private String userId;
    @Column(name = "from_status")
    private String fromStatus;
    @Column(name = "to_status", nullable = false)
    private String toStatus;
    @Column(columnDefinition = "TEXT")
    private String note;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @PrePersist
    public void onCreate(){ createdAt=Instant.now(); }
}
