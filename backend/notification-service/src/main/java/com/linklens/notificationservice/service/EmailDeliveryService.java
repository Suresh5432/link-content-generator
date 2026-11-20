package com.linklens.notificationservice.service;

import com.linklens.notificationservice.client.UserServiceClient;
import com.linklens.notificationservice.entity.EmailDelivery;
import com.linklens.notificationservice.entity.enums.EmailDeliveryStatus;
import com.linklens.notificationservice.repository.EmailDeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailDeliveryService {

    private final EmailDeliveryRepository emailDeliveryRepository;

    private final UserServiceClient userServiceClient;

    private final EmailSender emailSender;

    @Transactional
    public void processDelivery(UUID deliveryId){

        EmailDelivery delivery=emailDeliveryRepository.findById(deliveryId)
                .orElseThrow(()->
                        new IllegalArgumentException(
                                "Email delivery nor found: "+deliveryId
                        ));

        if (delivery.getStatus()!= EmailDeliveryStatus.PENDING
                && delivery.getStatus()!= EmailDeliveryStatus.RETRY) {
            return;
        }

        delivery.markSending();

        emailDeliveryRepository.saveAndFlush(delivery);

        try{
            var notification=delivery.getNotification();

            var profile=userServiceClient.getUserProfile(
                    notification.getUserId()
            );

            if (profile==null ||
                    profile.email()==null ||
                    profile.email().isBlank()) {
                throw new IllegalStateException(
                        "User email address is not available"
                );
            }

            delivery.setRecipientEmail(profile.email());

            emailSender.send(profile.email(),
                    notification.getTitle(),
                    notification.getMessage());

            delivery.markSent(null);

            log.info("Email delivered for notification {}", notification.getId());
        }
        catch (Exception e){

            long delaySeconds=Math.min(
                    3600,
                    30L * (1L<<Math.min(delivery.getAttemptCount()-1,7))
            );

            delivery.scheduleRetry(
                    e.getMessage(),
                    Instant.now().plusSeconds(delaySeconds)
            );

            log.error(
                    "Email delivery failed. deliveryId={}, attempts={}",
                    deliveryId,
                    delivery.getAttemptCount(),
                    e
            );
        }
    }
}
