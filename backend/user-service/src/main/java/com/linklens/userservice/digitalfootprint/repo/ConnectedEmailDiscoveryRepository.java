package com.linklens.userservice.digitalfootprint.repo;

import com.linklens.userservice.digitalfootprint.entity.ConnectedEmailDiscovery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConnectedEmailDiscoveryRepository extends JpaRepository<ConnectedEmailDiscovery, UUID> {
    List<ConnectedEmailDiscovery> findAllByUserIdOrderByLastSeenAtDesc(String userId);
    void deleteAllByConnectedAccountIdAndUserId(UUID connectedAccountId, String userId);
}
