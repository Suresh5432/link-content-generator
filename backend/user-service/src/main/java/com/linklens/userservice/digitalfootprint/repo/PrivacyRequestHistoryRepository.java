package com.linklens.userservice.digitalfootprint.repo;

import com.linklens.userservice.digitalfootprint.entity.PrivacyRequestHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PrivacyRequestHistoryRepository extends JpaRepository<PrivacyRequestHistory, UUID> {
    List<PrivacyRequestHistory> findAllByRequestIdAndUserIdOrderByCreatedAtAsc(UUID requestId, String userId);
}
