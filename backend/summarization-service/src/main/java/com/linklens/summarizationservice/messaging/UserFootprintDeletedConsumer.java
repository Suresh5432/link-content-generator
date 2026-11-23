package com.linklens.summarizationservice.messaging;

import com.linkcontext.contracts.events.UserFootprintDeletedEvent;
import com.linklens.summarizationservice.config.RabbitMQConfig;
import com.linklens.summarizationservice.repo.SummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserFootprintDeletedConsumer {

    private final SummaryRepository summaryRepository;

    @RabbitListener(queues = RabbitMQConfig.USER_FOOTPRINT_DELETED_QUEUE)
    @Transactional
    public void consume(UserFootprintDeletedEvent event) {
        if (event == null || event.version() != 1 || event.userId() == null || event.userId().isBlank()) {
            log.warn("Ignoring unsupported or invalid user footprint deletion event");
            return;
        }
        long deleted = summaryRepository.deleteByUserId(event.userId());
        log.info("Deleted {} summarization records for user footprint deletion event {}", deleted, event.eventId());
    }
}
