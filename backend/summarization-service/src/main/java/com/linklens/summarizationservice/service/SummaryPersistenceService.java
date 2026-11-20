package com.linklens.summarizationservice.service;

import com.linkcontext.contracts.events.SummaryGeneratedEvent;
import com.linklens.summarizationservice.dto.SummaryResult;
import com.linklens.summarizationservice.entity.ProcessedEvent;
import com.linklens.summarizationservice.entity.Summary;
import com.linklens.summarizationservice.entity.SummaryOutboxEvent;
import com.linklens.summarizationservice.repo.ProcessedEventRepository;
import com.linklens.summarizationservice.repo.SummaryOutboxRepository;
import com.linklens.summarizationservice.repo.SummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.Setter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SummaryPersistenceService {

    private final SummaryRepository summaryRepository;

    private final ProcessedEventRepository processedEventRepository;

    private final SummaryOutboxRepository summaryOutboxRepository;

    private final ObjectMapper  objectMapper;

    @Transactional
    public void save(
            UUID sourceEventId,
            UUID linkId,
            UUID contentId,
            String userId,
            String title,
            SummaryResult result){
        if(processedEventRepository.claimEvent(sourceEventId, "content.extracted.v1") == 0){
            log.info("Skipping persistence for already processed summary event {}", sourceEventId);
            return;
        }

        if (summaryRepository.findByLinkId(linkId).isPresent()){
            throw new IllegalStateException("A summary already exists for this link");
        }

        Summary summary=Summary.builder()
                .id(UUID.randomUUID())
                .linkId(linkId)
                .contentId(contentId)
                .userId(userId)
                .title(title)
                .summary(result.summary())
                .keyTakeaways(result.keyTakeaways())
                .readingTimeMinutes(result.readingTimeMinutes())
                .wordCount(result.wordCount())
                .aiProvider(result.aiProvider())
                .aiModel(result.aiModel())
                .build();

        summaryRepository.save(summary);
        log.debug("Saved summary {} for link {}", summary.getId(), linkId);

        SummaryGeneratedEvent event=new SummaryGeneratedEvent(
                UUID.randomUUID(),
                linkId,
                userId,
                result.summary(),
                result.keyTakeaways(),
                result.readingTimeMinutes(),
                Instant.now(),
                1
        );

        try{
            String payload=objectMapper.writeValueAsString(event);

            summaryOutboxRepository.save(new SummaryOutboxEvent(
                    UUID.randomUUID(),
                    linkId,
                    "summary.generated.v1",
                    payload)
            );

            log.debug("Queued summary generated event for link {}", linkId);
        }
        catch (Exception exception){
            log.error("Unable to create summary generated outbox event for link {}", linkId, exception);
            throw new IllegalStateException("Unable to serialize summary event", exception);
        }
    }
}
