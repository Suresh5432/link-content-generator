package com.linklens.notificationservice.repository;

import com.linklens.notificationservice.entity.EmailDelivery;
import com.linklens.notificationservice.entity.enums.EmailDeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface EmailDeliveryRepository extends JpaRepository<EmailDelivery, UUID> {

    List<EmailDelivery>
    findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
            EmailDeliveryStatus status, Instant now);

    boolean existsByNotificationId(UUID notificationId);
}
