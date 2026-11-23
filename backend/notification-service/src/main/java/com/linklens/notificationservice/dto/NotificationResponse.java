package com.linklens.notificationservice.dto;
import com.linklens.notificationservice.entity.Notification;
import java.time.Instant;
import java.util.UUID;
public record NotificationResponse(UUID id, UUID linkId, UUID requestId, String type, String title, String message, boolean read, Instant createdAt) {
 public static NotificationResponse from(Notification n) { return new NotificationResponse(n.getId(), n.getLinkId(), n.getRequestId(), n.getNotificationType().name(), n.getTitle(), n.getMessage(), Boolean.TRUE.equals(n.getRead()), n.getCreatedAt()); }
}
