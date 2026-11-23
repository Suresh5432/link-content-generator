package com.linklens.userservice.digitalfootprint.repo;

import com.linklens.userservice.digitalfootprint.entity.UserPrivacyPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserPrivacyPreferenceRepository extends JpaRepository<UserPrivacyPreference, UUID> {
    Optional<UserPrivacyPreference> findByUserId(String userId);
}
