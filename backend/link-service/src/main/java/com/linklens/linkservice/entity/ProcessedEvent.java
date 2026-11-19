package com.linklens.linkservice.entity;


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
    @Column(name="event_id", nullable = false)
    private UUID eventId;

    @Column(name = "event_type", nullable = false,length = 150)
    private String eventType;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    @PrePersist
    public void onCreate(){
        if (processedAt == null) {
            processedAt = Instant.now();
        }
    }
}
