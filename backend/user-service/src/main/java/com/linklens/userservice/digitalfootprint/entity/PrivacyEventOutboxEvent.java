package com.linklens.userservice.digitalfootprint.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "privacy_event_outbox")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PrivacyEventOutboxEvent {
    @Id
    private UUID id;
    @Column(name = "user_id")
    private String userId;
    @Column(nullable = false)
    private String exchange;
    @Column(name = "routing_key", nullable = false)
    private String routingKey;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;
    @Column(nullable = false)
    private String status;
    @Column(nullable = false)
    private int attempts;
    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "published_at")
    private Instant publishedAt;
    @Column(name = "last_error", length = 1000)
    private String lastError;

    @PrePersist
    public void onCreate() {
        if (id == null) id = UUID.randomUUID();
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (nextAttemptAt == null) nextAttemptAt = now;
        if (status == null) status = "PENDING";
    }
}
