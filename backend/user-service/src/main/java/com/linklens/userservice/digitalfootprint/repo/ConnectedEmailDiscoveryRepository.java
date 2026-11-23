package com.linklens.userservice.digitalfootprint.repo;

import com.linklens.userservice.digitalfootprint.entity.ConnectedEmailDiscovery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConnectedEmailDiscoveryRepository extends JpaRepository<ConnectedEmailDiscovery, UUID> {
    List<ConnectedEmailDiscovery> findAllByUserIdOrderByLastSeenAtDesc(String userId);
    List<ConnectedEmailDiscovery> findAllByUserIdAndDismissedByUserFalseOrderByLastSeenAtDesc(String userId);
    java.util.Optional<ConnectedEmailDiscovery> findFirstByConnectedAccountIdAndDomainAndEvidenceType(UUID accountId, String domain, String evidenceType);
    java.util.Optional<ConnectedEmailDiscovery> findByIdAndUserId(UUID id, String userId);
    void deleteAllByConnectedAccountIdAndUserId(UUID connectedAccountId, String userId);
}
