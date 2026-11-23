package com.linklens.userservice.digitalfootprint.repo;

import com.linklens.userservice.digitalfootprint.entity.WebsitePrivacyContact;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WebsitePrivacyContactRepository extends JpaRepository<WebsitePrivacyContact, UUID> {
    Optional<WebsitePrivacyContact> findByUserIdAndTrackedWebsiteId(String userId, UUID trackedWebsiteId);
    List<WebsitePrivacyContact> findAllByUserIdOrderByUpdatedAtDesc(String userId);
}
