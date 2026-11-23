package com.linklens.notificationservice.service;

import com.linkcontext.contracts.events.ExtractionFailedEvent;
import com.linkcontext.contracts.events.PrivacyRequestReminderEvent;
import com.linkcontext.contracts.events.PrivacyRequestStatusChangedEvent;
import com.linkcontext.contracts.events.SummarizationFailedEvent;
import com.linkcontext.contracts.events.SummaryGeneratedEvent;
import com.linklens.notificationservice.entity.EmailDelivery;
import com.linklens.notificationservice.entity.Notification;
import com.linklens.notificationservice.entity.NotificationPreference;
import com.linklens.notificationservice.entity.enums.NotificationType;
import com.linklens.notificationservice.repository.EmailDeliveryRepository;
import com.linklens.notificationservice.repository.NotificationPreferenceRepository;
import com.linklens.notificationservice.repository.NotificationRepository;
import com.linklens.notificationservice.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationProcessor {

    private final NotificationRepository notificationRepository;

    private final EmailDeliveryRepository emailDeliveryRepository;

    private final NotificationPreferenceRepository preferenceRepository;

    private final ProcessedEventRepository processedEventRepository;

    @Transactional
    public void process(SummaryGeneratedEvent event){

        processEvent(
                event.eventId(),
                "SUMMARY_GENERATED",
                event.userId(),
                event.linkId(),
                null,
                NotificationType.LINK_SUMMARY_READY,
                "Your link summary is ready",
                "your submitted link has been processed successfully"
        );
    }

    @Transactional
    public void process(ExtractionFailedEvent event){

        if (event.retryable()){

            log.info("Skipping retryable extraction failure for link {}",
                    event.linkId());

            return;
        }

        processEvent(
                event.eventId(),
                "EXTRACTION_FAILED",
                event.userId(),
                event.linkId(),
                null,
                NotificationType.LINK_PROCESSING_FAILED,
                "Link extraction failed",
                "we couldn't extract content form your submitted link. "
        );
    }

    @Transactional
    public void process(SummarizationFailedEvent event){

        if (event.retryable()){

            log.info("Skipping retryable summarization failure for link {}",
                    event.linkId());

            return;
        }

        processEvent(
                event.eventId(),
                "SUMMARIZATION_FAILED",
                event.userId(),
                event.linkId(),
                null,
                NotificationType.LINK_PROCESSING_FAILED,
                "Summary generation failed",
                "We couldn't generate a summary for your submitted link. "
        );
    }

    @Transactional
    public void process(PrivacyRequestStatusChangedEvent event) {
        processEvent(
                event.eventId(),
                "PRIVACY_REQUEST_STATUS_CHANGED",
                event.userId(),
                null,
                event.requestId(),
                NotificationType.PRIVACY_REQUEST_UPDATE,
                "Privacy request updated",
                "your " + humanType(event.requestType()) + " request is now " + humanStatus(event.newStatus())
        );
    }

    @Transactional
    public void process(PrivacyRequestReminderEvent event) {
        processEvent(
                event.eventId(),
                "PRIVACY_REQUEST_REMINDER",
                event.userId(),
                null,
                event.requestId(),
                NotificationType.PRIVACY_REQUEST_REMINDER,
                "Privacy request needs follow-up",
                "your " + humanType(event.requestType()) + " request is still " + humanStatus(event.status())
                        + ". Consider following up with the website."
        );
    }

    private void processEvent(
            UUID eventId,
            String eventType,
            String userId,
            UUID linkId,
            UUID requestId,
            NotificationType notificationType,
            String title,
            String message){

        if (processedEventRepository.claimEvent(eventId, eventType) == 0){

            log.info("Duplicate event ignored: {}", eventId);

            return;
        }

        NotificationPreference preference=
                preferenceRepository.findById(userId).orElse(null);

        boolean inAppEnabled =preference==null
                || preference.isInAppEnabled();

        boolean emailEnabled =preference==null
                || preference.isEmailEnabled();

        Notification notification=Notification.builder()
                .eventId(eventId)
                .userId(userId)
                .linkId(linkId)
                .requestId(requestId)
                .notificationType(notificationType)
                .title(title)
                .message(message)
                .inAppVisible(inAppEnabled)
                .read(false)
                .build();

        notificationRepository.save(notification);

        if (emailEnabled){
            emailDeliveryRepository.save(
                    new EmailDelivery(notification, null)
            );
        }

        log.info(
                "Notification processed. eventId={}, userId={}, inApp={}, email={}",
                eventId,userId,inAppEnabled,emailEnabled
        );
    }

    private static String humanType(String requestType) {
        if (requestType == null) return "privacy";
        return requestType.toLowerCase().replace('_', ' ');
    }

    private static String humanStatus(String status) {
        if (status == null) return "updated";
        return status.toLowerCase().replace('_', ' ');
    }
}
