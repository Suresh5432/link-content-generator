package com.linklens.notificationservice.dto;
import com.linklens.notificationservice.entity.NotificationPreference;
import java.time.Instant;
public record NotificationPreferencesResponse(boolean inAppEnabled, boolean emailEnabled, Instant updatedAt) {
 public static NotificationPreferencesResponse from(NotificationPreference p){ return new NotificationPreferencesResponse(p.isInAppEnabled(),p.isEmailEnabled(),p.getUpdatedAt()); }
}
