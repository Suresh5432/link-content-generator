package com.linklens.userservice.digitalfootprint.repo;

import com.linklens.userservice.digitalfootprint.entity.ConnectedEmailAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConnectedEmailAccountRepository extends JpaRepository<ConnectedEmailAccount, UUID> {
    List<ConnectedEmailAccount> findAllByUserIdOrderByCreatedAtDesc(String userId);
    Optional<ConnectedEmailAccount> findByIdAndUserId(UUID id, String userId);
}
