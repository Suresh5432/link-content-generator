package com.linklens.userservice.digitalfootprint.repo;

import com.linklens.userservice.digitalfootprint.entity.PrivacyRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PrivacyRequestRepository extends JpaRepository<PrivacyRequest, UUID> {
    List<PrivacyRequest> findAllByUserIdOrderByCreatedAtDesc(String userId);
    Optional<PrivacyRequest> findByIdAndUserId(UUID id, String userId);
    long countByUserId(String userId);
    long countByUserIdAndStatus(String userId, String status);
    long countByUserIdAndStatusIn(String userId, Collection<String> statuses);
    List<PrivacyRequest> findByStatusInAndReminderAtBefore(Collection<String> statuses, Instant now);
    boolean existsByUserIdAndTrackedWebsiteIdAndRequestTypeAndStatusIn(String userId, UUID trackedWebsiteId, String requestType, Collection<String> statuses);
}
