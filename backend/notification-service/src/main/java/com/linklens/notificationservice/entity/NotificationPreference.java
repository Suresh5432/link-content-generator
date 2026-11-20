package com.linklens.notificationservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;


import java.time.Instant;

@Entity
@Table(name = "notification_preferences")
@Getter
@NoArgsConstructor
public class NotificationPreference {

    @Id
    @Column(name = "user_id", length = 255)
    private String userId;

    @Column(name = "in_app_enabled", nullable = false)
    private boolean inAppEnabled=true;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled=true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public NotificationPreference(String userId) {
        this.userId = userId;
    }

    public void updatePreferences(boolean inAppEnabled,
                                  boolean emailEnabled) {

        this.inAppEnabled=inAppEnabled;
        this.emailEnabled=emailEnabled;
        this.updatedAt=Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
}
