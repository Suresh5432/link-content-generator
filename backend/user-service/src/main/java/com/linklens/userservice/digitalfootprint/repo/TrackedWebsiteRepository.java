package com.linklens.userservice.digitalfootprint.repo;

import com.linklens.userservice.digitalfootprint.entity.TrackedWebsite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrackedWebsiteRepository extends JpaRepository<TrackedWebsite, UUID> {
    Page<TrackedWebsite> findAllByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    List<TrackedWebsite> findAllByUserIdOrderByCreatedAtDesc(String userId);
    Optional<TrackedWebsite> findByIdAndUserId(UUID id, String userId);
    Optional<TrackedWebsite> findFirstByUserIdAndDomain(String userId, String domain);
    long countByUserId(String userId);
}
