package com.linklens.userservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserProfile {

    @Id
    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    @Column(name = "display_name", length = 80)
    private String displayName;

    @Column(length = 500)
    private String bio;

    @Column(name = "avatar_url", length = 2048)
    private String avatarUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UserProfile(String userId) {
        this.userId = userId;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
    public void updateProfile(
            String displayName,
            String bio,
            String avatarUrl
    ) {
        if (displayName != null) {
            this.displayName = displayName;
        }

        if (bio != null) {
            this.bio = bio;
        }

        if (avatarUrl != null) {
            this.avatarUrl = avatarUrl;
        }
    }
}
