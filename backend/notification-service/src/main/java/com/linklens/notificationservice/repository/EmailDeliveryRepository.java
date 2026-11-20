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
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

@Repository
public interface EmailDeliveryRepository extends JpaRepository<EmailDelivery, UUID> {

    List<EmailDelivery>
    findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
            EmailDeliveryStatus status, Instant now);

    boolean existsByNotificationId(UUID notificationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from EmailDelivery e where e.id = :id")
    java.util.Optional<EmailDelivery> findByIdForUpdate(@Param("id") UUID id);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("update EmailDelivery e set e.status = com.linklens.notificationservice.entity.enums.EmailDeliveryStatus.RETRY, e.nextAttemptAt = :now, e.lastError = 'Recovered abandoned sending lease' where e.status = com.linklens.notificationservice.entity.enums.EmailDeliveryStatus.SENDING and e.updatedAt < :cutoff")
    int recoverStuck(@Param("cutoff") Instant cutoff, @Param("now") Instant now);

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
