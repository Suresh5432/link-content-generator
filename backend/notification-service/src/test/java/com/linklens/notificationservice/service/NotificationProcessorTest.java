package com.linklens.notificationservice.service;

import com.linkcontext.contracts.events.PrivacyRequestReminderEvent;
import com.linkcontext.contracts.events.PrivacyRequestStatusChangedEvent;
import com.linkcontext.contracts.events.SummaryGeneratedEvent;
import com.linklens.notificationservice.entity.Notification;
import com.linklens.notificationservice.entity.enums.NotificationType;
import com.linklens.notificationservice.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationProcessorTest {
 private NotificationRepository notifications;
 private EmailDeliveryRepository emails;
 private NotificationPreferenceRepository preferences;
 private ProcessedEventRepository events;
 private NotificationProcessor processor;
 @BeforeEach void setup(){notifications=mock(NotificationRepository.class);emails=mock(EmailDeliveryRepository.class);preferences=mock(NotificationPreferenceRepository.class);events=mock(ProcessedEventRepository.class);processor=new NotificationProcessor(notifications,emails,preferences,events);}
 @Test void duplicateEventCreatesNoNotificationOrEmail(){
  var event=event(); when(events.claimEvent(event.eventId(),"SUMMARY_GENERATED")).thenReturn(0);
  processor.process(event);
  verifyNoInteractions(notifications,emails,preferences);
 }
 @Test void newSummaryCreatesOwnedNotificationAndEmailDelivery(){
  var event=event(); when(events.claimEvent(event.eventId(),"SUMMARY_GENERATED")).thenReturn(1); when(preferences.findById("subject-1")).thenReturn(Optional.empty());
  processor.process(event);
  ArgumentCaptor<Notification> captor=ArgumentCaptor.forClass(Notification.class); verify(notifications).save(captor.capture());
  assertEquals("subject-1",captor.getValue().getUserId()); assertEquals(event.linkId(),captor.getValue().getLinkId()); assertEquals(NotificationType.LINK_SUMMARY_READY,captor.getValue().getNotificationType());
  verify(emails).save(any());
 }

 @Test void privacyStatusChangeCreatesRequestLinkedNotification(){
  var requestId=UUID.randomUUID();
  var event=new PrivacyRequestStatusChangedEvent(UUID.randomUUID(),requestId,"subject-1","DELETION","SUBMITTED","ACKNOWLEDGED",Instant.now(),1);
  when(events.claimEvent(event.eventId(),"PRIVACY_REQUEST_STATUS_CHANGED")).thenReturn(1); when(preferences.findById("subject-1")).thenReturn(Optional.empty());
  processor.process(event);
  ArgumentCaptor<Notification> captor=ArgumentCaptor.forClass(Notification.class); verify(notifications).save(captor.capture());
  assertEquals("subject-1",captor.getValue().getUserId()); assertNull(captor.getValue().getLinkId()); assertEquals(requestId,captor.getValue().getRequestId());
  assertEquals(NotificationType.PRIVACY_REQUEST_UPDATE,captor.getValue().getNotificationType()); assertTrue(captor.getValue().getMessage().contains("acknowledged"));
  verify(emails).save(any());
 }

 @Test void privacyReminderCreatesNotification(){
  var requestId=UUID.randomUUID();
  var event=new PrivacyRequestReminderEvent(UUID.randomUUID(),requestId,"subject-1","DELETION","SUBMITTED",Instant.now(),1);
  when(events.claimEvent(event.eventId(),"PRIVACY_REQUEST_REMINDER")).thenReturn(1); when(preferences.findById("subject-1")).thenReturn(Optional.empty());
  processor.process(event);
  ArgumentCaptor<Notification> captor=ArgumentCaptor.forClass(Notification.class); verify(notifications).save(captor.capture());
  assertEquals(requestId,captor.getValue().getRequestId()); assertEquals(NotificationType.PRIVACY_REQUEST_REMINDER,captor.getValue().getNotificationType());
  assertTrue(captor.getValue().getMessage().contains("following up"));
 }

 private SummaryGeneratedEvent event(){return new SummaryGeneratedEvent(UUID.randomUUID(),UUID.randomUUID(),"subject-1","summary",List.of("takeaway"),1,Instant.now(),1);}
}
