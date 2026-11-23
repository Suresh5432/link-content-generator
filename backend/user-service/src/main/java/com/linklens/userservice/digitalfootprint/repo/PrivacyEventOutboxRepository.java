package com.linklens.userservice.digitalfootprint.repo;

import com.linklens.userservice.digitalfootprint.entity.PrivacyEventOutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PrivacyEventOutboxRepository extends JpaRepository<PrivacyEventOutboxEvent, UUID> {

    @Query("select e from PrivacyEventOutboxEvent e where e.status = 'PENDING' and e.nextAttemptAt <= :now order by e.createdAt asc")
    List<PrivacyEventOutboxEvent> findPendingBatch(@Param("now") Instant now, Pageable pageable);

    List<PrivacyEventOutboxEvent> findAllByUserIdOrderByCreatedAtDesc(String userId);
}
