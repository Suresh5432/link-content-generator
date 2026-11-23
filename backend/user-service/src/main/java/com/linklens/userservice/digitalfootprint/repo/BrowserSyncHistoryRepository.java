package com.linklens.userservice.digitalfootprint.repo;

import com.linklens.userservice.digitalfootprint.entity.BrowserSyncHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface BrowserSyncHistoryRepository extends JpaRepository<BrowserSyncHistory, UUID> {
    List<BrowserSyncHistory> findAllByIntegrationIdAndUserIdOrderByCreatedAtDesc(UUID integrationId, String userId);
}
