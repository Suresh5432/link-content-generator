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
}
