package com.linklens.userservice.digitalfootprint.repo;

import com.linklens.userservice.digitalfootprint.entity.CookieInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CookieInventoryRepository extends JpaRepository<CookieInventory, UUID> {
    List<CookieInventory> findAllByUserIdOrderByDomainAscCookieNameAsc(String userId);
    List<CookieInventory> findAllByUserIdAndDomainOrderByCookieNameAsc(String userId, String domain);
    List<CookieInventory> findAllByUserIdAndTrackingRelatedTrueOrderByDomainAsc(String userId);
    Optional<CookieInventory> findByIdAndUserId(UUID id, String userId);
    Optional<CookieInventory> findByUserIdAndBrowserIntegrationIdAndDomainAndCookieName(String userId, UUID browserIntegrationId, String domain, String cookieName);
    List<CookieInventory> findAllByUserIdAndBrowserIntegrationId(String userId, UUID browserIntegrationId);
    long countByUserId(String userId);
    long countByUserIdAndTrackingRelatedTrue(String userId);

    @Query("select distinct c.domain from CookieInventory c where c.userId = :userId order by c.domain")
    List<String> findDistinctDomains(@Param("userId") String userId);
}
