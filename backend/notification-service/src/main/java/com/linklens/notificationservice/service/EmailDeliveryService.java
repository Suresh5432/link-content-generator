package com.linklens.notificationservice.service;

import com.linklens.notificationservice.client.UserServiceClient;
import com.linklens.notificationservice.entity.EmailDelivery;
import com.linklens.notificationservice.entity.enums.EmailDeliveryStatus;
import com.linklens.notificationservice.repository.EmailDeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Instant;
import java.util.UUID;

@Service @RequiredArgsConstructor @Slf4j
public class EmailDeliveryService {
 private final EmailDeliveryRepository repository;
 private final UserServiceClient userServiceClient;
 private final EmailSender emailSender;
 private final TransactionTemplate transactionTemplate;

 public void processDelivery(UUID id) {
  DeliveryData data = transactionTemplate.execute(status -> {
   EmailDelivery d = repository.findByIdForUpdate(id).orElse(null);
   if (d == null || (d.getStatus()!=EmailDeliveryStatus.PENDING && d.getStatus()!=EmailDeliveryStatus.RETRY)) return null;
   d.markSending();
   repository.save(d);
   var n=d.getNotification();
   return new DeliveryData(n.getUserId(), n.getTitle(), n.getMessage(), n.getId(), d.getAttemptCount());
  });
  if (data == null) return;
  try {
   var profile=userServiceClient.getUserProfile(data.userId());
   if(profile==null || profile.email()==null || profile.email().isBlank()) throw new IllegalStateException("User email address is not available");
   emailSender.send(profile.email(), data.title(), data.message());
   transactionTemplate.executeWithoutResult(status -> repository.findByIdForUpdate(id).ifPresent(d -> { d.setRecipientEmail(profile.email()); d.markSent(null); }));
   log.info("Email delivered for notification {}", data.notificationId());
  } catch(Exception e) {
   transactionTemplate.executeWithoutResult(status -> repository.findByIdForUpdate(id).ifPresent(d -> {
    long delay=Math.min(3600,30L*(1L<<Math.min(d.getAttemptCount()-1,7)));
    d.scheduleRetry(e.getMessage()==null?e.getClass().getSimpleName():e.getMessage(), Instant.now().plusSeconds(delay));
   }));
   log.error("Email delivery failed. deliveryId={}, attempts={}",id,data.attempt(),e);
  }
 }
 private record DeliveryData(String userId,String title,String message,UUID notificationId,int attempt) {}
}
