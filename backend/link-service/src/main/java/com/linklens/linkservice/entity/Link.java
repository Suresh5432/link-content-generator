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
        this.status=status;
        this.failureCode=failureCode;
        this.completedAt=null;
    }

    public void softDelete(){
        this.deletedAt=Instant.now();
    }
}
