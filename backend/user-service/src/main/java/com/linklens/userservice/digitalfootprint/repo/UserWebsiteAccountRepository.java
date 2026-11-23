package com.linklens.userservice.digitalfootprint.repo;

import com.linklens.userservice.digitalfootprint.entity.UserWebsiteAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserWebsiteAccountRepository extends JpaRepository<UserWebsiteAccount, UUID> {
    List<UserWebsiteAccount> findAllByUserIdAndTrackedWebsiteIdOrderByCreatedAtDesc(String userId, UUID trackedWebsiteId);
    List<UserWebsiteAccount> findAllByUserIdOrderByCreatedAtDesc(String userId);
    Optional<UserWebsiteAccount> findByIdAndUserId(UUID id, String userId);
    long countByUserId(String userId);
}
