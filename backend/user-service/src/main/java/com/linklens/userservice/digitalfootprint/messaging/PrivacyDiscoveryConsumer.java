package com.linklens.userservice.digitalfootprint.messaging;

import com.linkcontext.contracts.events.PrivacyDiscoveryDiscoveredEvent;
import com.linklens.userservice.digitalfootprint.service.PrivacyDiscoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PrivacyDiscoveryConsumer {

    private final PrivacyDiscoveryService discoveryService;

    @RabbitListener(queues = PrivacyDiscoveryMessagingConfig.PRIVACY_DISCOVERY_QUEUE)
    public void consume(PrivacyDiscoveryDiscoveredEvent event) {
        log.info("Received privacy discovery event {} (version {})", event.eventId(), event.version());
        discoveryService.ingest(event);
    }
}
