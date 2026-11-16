package com.linklens.extractionservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_events")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProcessedEvent {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name="processed_at", nullable = false)
    private Instant processedAt;

    @PrePersist
    protected  void prePersist(){
        if (this.processedAt == null) {
            this.processedAt = Instant.now();
        }
    }

}
