package com.linklens.userservice.digitalfootprint.repo;

import com.linklens.userservice.digitalfootprint.entity.PrivacyDiscoveryFinding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PrivacyDiscoveryFindingRepository extends JpaRepository<PrivacyDiscoveryFinding, UUID> {
    List<PrivacyDiscoveryFinding> findAllByUserIdAndDomainOrderByCreatedAtAsc(String userId, String domain);
    List<PrivacyDiscoveryFinding> findAllByUserIdOrderByDomainAscCreatedAtAsc(String userId);
    boolean existsByUserIdAndDomainAndFindingTypeAndSourceUrl(String userId, String domain, String findingType, String sourceUrl);
}
