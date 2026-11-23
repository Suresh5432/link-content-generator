package com.linklens.userservice.digitalfootprint.repo;

import com.linklens.userservice.digitalfootprint.entity.BrowserIntegration;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BrowserIntegrationRepository extends JpaRepository<BrowserIntegration, UUID> {
    Optional<BrowserIntegration> findByUserIdAndInstallationId(String userId, String installationId);
    Optional<BrowserIntegration> findByIdAndUserId(UUID id, String userId);
    List<BrowserIntegration> findAllByUserIdOrderByCreatedAtDesc(String userId);
}
