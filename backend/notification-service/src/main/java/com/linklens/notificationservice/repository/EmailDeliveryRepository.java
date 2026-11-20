package com.linklens.notificationservice.repository;

import com.linklens.notificationservice.entity.EmailDelivery;
import com.linklens.notificationservice.entity.enums.EmailDeliveryStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface EmailDeliveryRepository extends JpaRepository<EmailDelivery, UUID> {

    List<EmailDelivery>
    findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
            EmailDeliveryStatus status, Instant now);

    boolean existsByNotificationId(UUID notificationId);

    @Query("""
    SELECT e FROM EmailDelivery e
    WHERE e.status IN :statuses
      AND (e.nextAttemptAt IS NULL OR e.nextAttemptAt <= :now)
    ORDER BY e.createdAt ASC
    """)
    List<EmailDelivery> findDueDeliveries(
            @Param("statuses") Set<EmailDeliveryStatus> statuses,
            @Param("now") Instant now,
            Pageable pageable
    );
}
