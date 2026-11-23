package com.linklens.userservice.digitalfootprint.repo;

import com.linklens.userservice.digitalfootprint.entity.ConnectedEmailOAuthState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import jakarta.persistence.LockModeType;

public interface ConnectedEmailOAuthStateRepository extends JpaRepository<ConnectedEmailOAuthState, String> {
    void deleteAllByExpiresAtBefore(Instant instant);
    void deleteAllByUserId(String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ConnectedEmailOAuthState s where s.stateHash = :stateHash")
    java.util.Optional<ConnectedEmailOAuthState> findByStateHashForUpdate(@Param("stateHash") String stateHash);
}
