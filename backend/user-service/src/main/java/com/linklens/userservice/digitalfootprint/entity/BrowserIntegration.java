package com.linklens.userservice.digitalfootprint.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "browser_integrations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BrowserIntegration {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private String userId;
    @Column(name = "installation_id", nullable = false)
    private String installationId;
    @Column(name = "browser_name")
    private String browserName;
    @Column(name = "extension_version")
    private String extensionVersion;
    @Column(name = "device_label")
    private String deviceLabel;
    @Column(name = "last_sync_at")
    private Instant lastSyncAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @PrePersist
    public void onCreate(){ createdAt=Instant.now(); updatedAt=Instant.now(); }
    @PreUpdate
    public void onUpdate(){ updatedAt=Instant.now(); }
}
