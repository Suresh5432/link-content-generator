package com.linklens.linkservice.entity;

import com.linklens.linkservice.enums.LinkStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="links")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Link {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false,length = 2048)
    private String url;

    @Column(name="user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private LinkStatus linkStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void onCreate(){
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }
    @PreUpdate
    public void onUpdate(){
        updatedAt = Instant.now();
    }

}
