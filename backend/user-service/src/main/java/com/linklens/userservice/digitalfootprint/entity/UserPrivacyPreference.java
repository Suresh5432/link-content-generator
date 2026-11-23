package com.linklens.userservice.digitalfootprint.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_privacy_preferences")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserPrivacyPreference {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;
    @Column(name = "analytics_cookies", nullable = false)
    private boolean analyticsCookies;
    @Column(name = "advertising_cookies", nullable = false)
    private boolean advertisingCookies;
    @Column(name = "marketing_communications", nullable = false)
    private boolean marketingCommunications;
    @Column(name = "third_party_tracking", nullable = false)
    private boolean thirdPartyTracking;
    @Column(name = "personalized_advertising", nullable = false)
    private boolean personalizedAdvertising;
    @Column(name = "data_sharing", nullable = false)
    private boolean dataSharing;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @PrePersist @PreUpdate
    public void touch(){ updatedAt=Instant.now(); }
}
