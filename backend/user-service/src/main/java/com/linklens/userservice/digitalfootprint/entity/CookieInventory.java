package com.linklens.userservice.digitalfootprint.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cookie_inventory")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CookieInventory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private String userId;
    @Column(name = "tracked_website_id")
    private UUID trackedWebsiteId;
    @Column(name = "browser_integration_id")
    private UUID browserIntegrationId;
    @Column(name = "cookie_name", nullable = false)
    private String cookieName;
    @Column(nullable = false)
    private String domain;
    @Column(name = "first_party", nullable = false)
    private boolean firstParty;
    private Instant expiration;
    private String purpose;
    @Column(nullable = false)
    private String category;
    @Column(nullable = false)
    private boolean secure;
    @Column(name = "http_only", nullable = false)
    private boolean httpOnly;
    @Column(name = "same_site")
    private String sameSite;
    @Column(name = "tracking_related", nullable = false)
    private boolean trackingRelated;
    @Column(nullable = false)
    private String source;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @PrePersist
    public void onCreate(){ createdAt=Instant.now(); updatedAt=Instant.now(); }
    @PreUpdate
    public void onUpdate(){ updatedAt=Instant.now(); }
}
