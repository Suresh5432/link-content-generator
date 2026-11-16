package com.linklens.extractionservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "extraction_outbox_events")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExtractionOutboxEvent {

    @Id
    private UUID id;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 150)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false,columnDefinition = "jsonb")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "available_at", nullable = false)
    private Instant availableAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "last_error")
    private String lastError;

    @PrePersist
    protected void onCreate(){
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (availableAt==null){
            availableAt = now;
        }
    }

    public void markPublished(){
        this.publishedAt = Instant.now();
        this.lastError=null;
    }

    public void recordFailure(String error){
        this.attempts++;

        this.lastError=error.length()>2000
                ? error.substring(0,2000)
                : error;

        long delay=Math.min(5L* (1L<<Math.min(attempts-1,10)),3600L);

        this.availableAt=Instant.now().plusSeconds(delay);
    }
}
