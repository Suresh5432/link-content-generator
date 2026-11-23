package com.linklens.extractionservice.messaging;

import com.linkcontext.contracts.events.UserFootprintDeletedEvent;
import com.linklens.extractionservice.config.RabbitMQConfig;
import com.linklens.extractionservice.repo.ExtractionContentRepository;
import com.linklens.extractionservice.websiteanalysis.repo.WebsiteAnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserFootprintDeletedConsumer {

    private final ExtractionContentRepository contentRepository;
    private final WebsiteAnalysisRepository websiteAnalysisRepository;

    @RabbitListener(queues = RabbitMQConfig.USER_FOOTPRINT_DELETED_QUEUE)
    @Transactional
    public void consume(UserFootprintDeletedEvent event) {
        if (event == null || event.version() != 1 || event.userId() == null || event.userId().isBlank()) {
            log.warn("Ignoring unsupported or invalid user footprint deletion event");
            return;
        }
        long content = contentRepository.deleteByUserId(event.userId());
        long analyses = websiteAnalysisRepository.deleteByUserId(event.userId());
        log.info("Deleted {} extraction content rows and {} website analyses for user footprint deletion event {}",
                content, analyses, event.eventId());
    }
}
