package com.linklens.userservice.digitalfootprint.repo;

import com.linklens.userservice.digitalfootprint.entity.PrivacyRequestEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PrivacyRequestEvidenceRepository extends JpaRepository<PrivacyRequestEvidence, UUID> {
    List<PrivacyRequestEvidence> findAllByRequestIdAndUserIdOrderByCreatedAtAsc(UUID requestId, String userId);
}
