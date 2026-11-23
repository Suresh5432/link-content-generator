package com.linklens.userservice.digitalfootprint.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "website_privacy_contacts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WebsitePrivacyContact {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private String userId;
    @Column(name = "tracked_website_id")
    private UUID trackedWebsiteId;
    @Column(name = "privacy_policy_url", columnDefinition = "TEXT")
    private String privacyPolicyUrl;
    @Column(name = "deletion_policy_url", columnDefinition = "TEXT")
    private String deletionPolicyUrl;
    @Column(name = "access_request_url", columnDefinition = "TEXT")
    private String accessRequestUrl;
    @Column(name = "account_deletion_url", columnDefinition = "TEXT")
    private String accountDeletionUrl;
    @Column(name = "cookie_management_url", columnDefinition = "TEXT")
    private String cookieManagementUrl;
    @Column(name = "consent_management_url", columnDefinition = "TEXT")
    private String consentManagementUrl;
    @Column(name = "privacy_contact_email")
    private String privacyContactEmail;
    @Column(name = "dpo_contact", columnDefinition = "TEXT")
    private String dpoContact;
    @Column(name = "source_urls", columnDefinition = "TEXT")
    private String sourceUrls;
    @Column(name = "discovered_at", nullable = false)
    private Instant discoveredAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @PrePersist
    public void onCreate(){ discoveredAt=Instant.now(); updatedAt=Instant.now(); }
    @PreUpdate
    public void onUpdate(){ updatedAt=Instant.now(); }
}
