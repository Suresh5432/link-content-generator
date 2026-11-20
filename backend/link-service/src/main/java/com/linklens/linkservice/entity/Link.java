package com.linklens.linkservice.entity;

import com.linklens.linkservice.enums.LinkStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="links",
    indexes = {
        @Index(
                name="idx_links_user_created",
                columnList = "user_id, created_at"
        ),
            @Index(
                 name = "idx_links_status",
                 columnList = "status"
            )
    })
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Link {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name="user_id", nullable = false, length = 255)
    private String userId;

    @Column(name = "original_url", nullable = false,columnDefinition = "TEXT")
    private String originalUrl;

    @Column(name = "normalized_url", nullable = false,columnDefinition = "TEXT")
    private String normalizedUrl;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private LinkStatus status;

    @Column(name = "failure_code", length = 100)
    private String failureCode;

    @Column(name="idempotency_key", length = 120)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    @Column(name="version", nullable = false)
    private Long version;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PrePersist
    public void onCreate(){
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (this.status==null) this.status=LinkStatus.SUBMITTED;
    }
    @PreUpdate
    public void onUpdate(){
        updatedAt = Instant.now();
    }

    public void updateStatus(LinkStatus status){
        if (status == null || !canTransition(this.status, status)) {
            throw new IllegalStateException("Invalid link status transition: " + this.status + " -> " + status);
        }
        this.status=status;
        this.failureCode=null;
        if(status==LinkStatus.COMPLETED){
            this.completedAt=Instant.now();
        }
        else{
            this.completedAt=null;
        }
    }

    public void markFailed(LinkStatus status,String failureCode){
        if ((status != LinkStatus.EXTRACTION_FAILED && status != LinkStatus.SUMMARIZATION_FAILED && status != LinkStatus.FAILED)
                || !canTransition(this.status, status)) {
            throw new IllegalStateException("Invalid link failure transition: " + this.status + " -> " + status);
        }
        this.status=status;
        this.failureCode=failureCode;
        this.completedAt=null;
    }

    public void softDelete(){
        this.deletedAt=Instant.now();
    }

    private boolean canTransition(LinkStatus current, LinkStatus target) {
        if (current == null) return target == LinkStatus.SUBMITTED || target == LinkStatus.PENDING_EXTRACTION;
        return switch (current) {
            case SUBMITTED -> target == LinkStatus.PENDING_EXTRACTION || target == LinkStatus.EXTRACTING || target == LinkStatus.EXTRACTION_FAILED || target == LinkStatus.FAILED;
            case PENDING_EXTRACTION -> target == LinkStatus.EXTRACTING || target == LinkStatus.EXTRACTED || target == LinkStatus.SUMMARIZING || target == LinkStatus.COMPLETED || target == LinkStatus.EXTRACTION_FAILED || target == LinkStatus.SUMMARIZATION_FAILED || target == LinkStatus.FAILED;
            case EXTRACTING -> target == LinkStatus.EXTRACTED || target == LinkStatus.SUMMARIZING || target == LinkStatus.COMPLETED || target == LinkStatus.EXTRACTION_FAILED || target == LinkStatus.FAILED;
            case EXTRACTED -> target == LinkStatus.SUMMARIZING || target == LinkStatus.COMPLETED || target == LinkStatus.SUMMARIZATION_FAILED || target == LinkStatus.FAILED;
            case SUMMARIZING -> target == LinkStatus.COMPLETED || target == LinkStatus.SUMMARIZATION_FAILED || target == LinkStatus.FAILED;
            case EXTRACTION_FAILED, SUMMARIZATION_FAILED, FAILED -> target == LinkStatus.PENDING_EXTRACTION;
            case COMPLETED -> false;
        };
    }
}
