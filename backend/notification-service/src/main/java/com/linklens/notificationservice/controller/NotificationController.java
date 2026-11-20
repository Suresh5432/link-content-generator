package com.linklens.notificationservice.controller;

import com.linklens.notificationservice.dto.*;
import com.linklens.notificationservice.service.NotificationQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;

@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/notifications")
public class NotificationController {
 private final NotificationQueryService service;
 @GetMapping public Page<NotificationResponse> list(@AuthenticationPrincipal Jwt jwt,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam(defaultValue="DESC") Sort.Direction direction){return service.list(jwt.getSubject(),page,size,direction);}
 @GetMapping("/unread-count") public Map<String,Long> unreadCount(@AuthenticationPrincipal Jwt jwt){return Map.of("count",service.unreadCount(jwt.getSubject()));}
 @PatchMapping("/{id}/read") public ResponseEntity<Void> markRead(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){service.markRead(jwt.getSubject(),id);return ResponseEntity.noContent().build();}
 @PatchMapping("/read-all") public Map<String,Integer> markAllRead(@AuthenticationPrincipal Jwt jwt){return Map.of("updated",service.markAllRead(jwt.getSubject()));}
 @GetMapping("/preferences") public NotificationPreferencesResponse preferences(@AuthenticationPrincipal Jwt jwt){return service.getPreferences(jwt.getSubject());}
 @PutMapping("/preferences") public NotificationPreferencesResponse preferences(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody NotificationPreferencesRequest request){return service.updatePreferences(jwt.getSubject(),request);}
}
