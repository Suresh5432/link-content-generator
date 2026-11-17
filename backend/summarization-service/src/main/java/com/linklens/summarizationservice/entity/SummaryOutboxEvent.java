package com.linklens.summarizationservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "summary_outbox_events")
public class SummaryOutboxEvent {
    @Getter
    @Id
    private UUID id;

    @Getter
    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Getter
    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Getter
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "available_at", nullable = false)
    private Instant availableAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(nullable = false)
    private int attempts;

    protected  SummaryOutboxEvent() {}

    @Column(name="last_error")
    private String lastError;

    public SummaryOutboxEvent(
            UUID id,
            UUID aggregateId,
            String eventType,
            String payload
    ) {
        this.id = id;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.attempts = 0;
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt=Instant.now();
        if (availableAt == null) availableAt=Instant.now();
    }

    public void markPublished(){
        publishedAt=Instant.now();
        lastError=null;
    }

    public void recordFailure(String error){
        attempts++;

        lastError=error==null ? "Unknown error"
                    : error.substring(0,Math.min(error.length(),2000));

        long delay=Math.min(5L*(1L<<Math.min(attempts-1, 10)),3600L);

        availableAt=Instant.now().plusSeconds(delay);
    }

}
