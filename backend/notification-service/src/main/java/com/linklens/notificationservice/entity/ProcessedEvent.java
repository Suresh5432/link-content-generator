package com.linklens.notificationservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.CodePointLength;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_events")
@Getter
@NoArgsConstructor
public class ProcessedEvent {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "event_type", nullable = false,length = 100)
    private String eventType;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    public ProcessedEvent(UUID eventId, String eventType) {
        this.eventId = eventId;
        this.eventType = eventType;
    }

    @PrePersist
    protected void onCreate() {
        this.processedAt = Instant.now();
    }
}
