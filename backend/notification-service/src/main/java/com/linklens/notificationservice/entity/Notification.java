package com.linklens.notificationservice.entity;

import com.linklens.notificationservice.entity.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name="notifications",
        indexes = {
                @Index(
                        name = "idx_notifications_user_created",
                        columnList = "user_id, created_at"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    private UUID id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    @Column(name = "link_id")
    private UUID linkId;

    @Column(name = "request_id")
    private UUID requestId;

    @Enumerated(EnumType.STRING)
    @Column(name="notification_type", nullable = false, length = 40)
    private NotificationType notificationType;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(name = "in_app_visible", nullable = false)
    private Boolean inAppVisible=true;

    @Column(name = "is_read", nullable = false)
    private Boolean read=false;

    @Column(name="read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void markAsRead(){
        if (!Boolean.TRUE.equals(this.read)){
            this.read=true;
            this.readAt=Instant.now();
            this.updatedAt=Instant.now();
        }
    }

    @PrePersist
    public void prePersist(){
        if (id == null){
            id = UUID.randomUUID();
        }
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

}
