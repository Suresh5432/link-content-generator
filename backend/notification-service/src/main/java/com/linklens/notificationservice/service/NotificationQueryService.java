package com.linklens.notificationservice.service;

import com.linklens.notificationservice.dto.NotificationPreferencesRequest;
import com.linklens.notificationservice.dto.NotificationResponse;
import com.linklens.notificationservice.dto.NotificationPreferencesResponse;
import com.linklens.notificationservice.repository.NotificationPreferenceRepository;
import com.linklens.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class NotificationQueryService {
 private final NotificationRepository notifications;
 private final NotificationPreferenceRepository preferences;
 @Transactional(readOnly=true) public Page<NotificationResponse> list(String userId,int page,int size,Sort.Direction direction) {
  validate(userId,page,size);
  return notifications.findByUserIdAndInAppVisibleTrueOrderByCreatedAtDesc(userId,PageRequest.of(page,size,Sort.by(direction,"createdAt"))).map(NotificationResponse::from);
 }
 @Transactional(readOnly=true) public long unreadCount(String userId) { validate(userId,0,1); return notifications.countByUserIdAndInAppVisibleTrueAndReadFalse(userId); }
 @Transactional public void markRead(String userId,UUID id) {
  notifications.findByIdAndUserIdAndInAppVisibleTrue(id,userId).orElseThrow(()->new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Notification not found")).markAsRead();
 }
 @Transactional public int markAllRead(String userId) { if(userId==null||userId.isBlank()) throw new IllegalArgumentException("Authenticated user is required"); return notifications.markAllRead(userId); }
 @Transactional(readOnly=true) public NotificationPreferencesResponse getPreferences(String userId) { return NotificationPreferencesResponse.from(preferences.findById(userId).orElseGet(()->new com.linklens.notificationservice.entity.NotificationPreference(userId))); }
 @Transactional public NotificationPreferencesResponse updatePreferences(String userId,NotificationPreferencesRequest request) {
  if(userId==null||userId.isBlank()) throw new IllegalArgumentException("Authenticated user is required");
  var p=preferences.findById(userId).orElseGet(()->new com.linklens.notificationservice.entity.NotificationPreference(userId));
  p.updatePreferences(request.inAppEnabled(),request.emailEnabled()); return NotificationPreferencesResponse.from(preferences.save(p));
 }
 private void validate(String user,int page,int size) { if(user==null||user.isBlank()) throw new IllegalArgumentException("Authenticated user is required"); if(page<0||size<1||size>100) throw new IllegalArgumentException("Invalid pagination"); }
}
