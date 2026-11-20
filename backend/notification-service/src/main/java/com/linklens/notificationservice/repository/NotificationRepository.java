package com.linklens.notificationservice.repository;

import com.linklens.notificationservice.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Optional<Notification> findByEventId(UUID eventId);

    Page<Notification> findByUserIdAndInAppVisibleTrueOrderByCreatedAtDesc(String userId, Pageable pageable);

    Page<Notification> findByUserIdAndInAppVisibleTrueAndReadFalseOrderByCreatedAtDesc(String userId, Pageable pageable);

    long countByUserIdAndInAppVisibleTrueAndReadFalse(String UserId);

    Optional<Notification> findByIdAndUserIdAndInAppVisibleTrue(UUID id, String userId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update Notification n set n.read = true, n.readAt = CURRENT_TIMESTAMP, n.updatedAt = CURRENT_TIMESTAMP where n.userId = :userId and n.inAppVisible = true and n.read = false")
    int markAllRead(@org.springframework.data.repository.query.Param("userId") String userId);
}
