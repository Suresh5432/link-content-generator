package com.linklens.notificationservice.entity;

import com.linklens.notificationservice.entity.enums.EmailDeliveryStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="email_deliveries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailDelivery {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "notification_id",
            nullable = false,
            unique = true)
    private Notification notification;

    @Column(name = "recipient_email", length = 320)
    private String recipientEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EmailDeliveryStatus status=EmailDeliveryStatus.PENDING;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount=0;

    @Column(name="max_attempts", nullable = false)
    private int maxAttempts=5;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name="sent_at")
    private Instant sentAt;

    @Column(name = "provider_message_id")
    private String providerMessageId;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public EmailDelivery(Notification notification,
                         String recipientEmail) {
        this.notification = notification;
        this.recipientEmail=recipientEmail;
        this.nextAttemptAt = Instant.now();
    }

    public void markSending(){
        this.status=EmailDeliveryStatus.SENDING;
        this.attemptCount++;
        this.nextAttemptAt = null;
        this.updatedAt=Instant.now();
    }

    public void markSent(String providerMessageId) {
        this.status=EmailDeliveryStatus.SENT;
        this.sentAt=Instant.now();
        this.providerMessageId=providerMessageId;
        this.lastError=null;
        this.nextAttemptAt=null;
        this.updatedAt=Instant.now();
    }

    public void scheduleRetry(String error, Instant nextAttemptAt) {
        this.lastError=error;
        this.updatedAt=Instant.now();

        if(attemptCount>=maxAttempts){
            this.status=EmailDeliveryStatus.DEAD;
            this.nextAttemptAt=null;
        }
        else{
            this.status=EmailDeliveryStatus.RETRY;
            this.nextAttemptAt=nextAttemptAt;
        }
    }

    public void suppress(){
        this.status=EmailDeliveryStatus.SUPPRESSED;
        this.updatedAt=Instant.now();
    }

    @PrePersist
    protected void prePersist(){
        if (this.id==null){
            this.id=UUID.randomUUID();
        }
        this.createdAt=Instant.now();
        this.updatedAt=Instant.now();

    }
}
