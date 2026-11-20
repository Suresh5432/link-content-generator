package com.linklens.notificationservice.messaging;

import com.linklens.notificationservice.entity.enums.EmailDeliveryStatus;
import com.linklens.notificationservice.repository.EmailDeliveryRepository;
import com.linklens.notificationservice.service.EmailDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailDeliveryWorker {

    private final EmailDeliveryRepository emailDeliveryRepository;

    private final EmailDeliveryService emailDeliveryService;

    @Value("${notification.worker.batch-size}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${notification.worker.poll-interval-ms}")
    public void processPendingEmails(){
        emailDeliveryRepository.recoverStuck(Instant.now().minusSeconds(300), Instant.now());
        var deliveries =emailDeliveryRepository.findDueDeliveries(
                Set.of(
                        EmailDeliveryStatus.PENDING,
                        EmailDeliveryStatus.RETRY
                ), Instant.now(),
                PageRequest.of(0,batchSize)
        );

        if (deliveries.isEmpty()) {
            return;
        }

        log.info("Found {} email deliveries to process ", deliveries.size());

        deliveries.forEach(delivery -> {
            try{
                emailDeliveryService.processDelivery(delivery.getId());
            }
            catch(Exception e){
                log.error("Unexpected email worker error for delivery {} ",delivery.getId(), e);
            }
        });
    }
}
