package com.linklens.notificationservice.entity;

import com.linklens.notificationservice.entity.enums.EmailDeliveryStatus;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class NotificationStateTest {
 @Test void preferencesCanDisableBothChannels() {
  NotificationPreference p = new NotificationPreference("subject");
  p.updatePreferences(false, false);
  assertFalse(p.isInAppEnabled());
  assertFalse(p.isEmailEnabled());
 }
 @Test void emailAttemptsStopAtConfiguredLimit() {
  Notification n = Notification.builder().build();
  EmailDelivery d = new EmailDelivery(n, null);
  for (int i=0;i<5;i++) d.markSending();
  d.scheduleRetry("smtp failed", Instant.now().plusSeconds(30));
  assertEquals(EmailDeliveryStatus.DEAD,d.getStatus());
  assertNull(d.getNextAttemptAt());
 }
 @Test void emailRetryUsesRetryStateUntilLimit() {
  Notification n = Notification.builder().build();
  EmailDelivery d = new EmailDelivery(n,null);
  d.markSending(); d.scheduleRetry("temporary",Instant.now().plusSeconds(30));
  assertEquals(EmailDeliveryStatus.RETRY,d.getStatus());
  assertNotNull(d.getNextAttemptAt());
 }
}
