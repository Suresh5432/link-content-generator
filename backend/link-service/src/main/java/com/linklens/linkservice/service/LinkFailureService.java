package com.linklens.linkservice.service;

import com.linklens.linkservice.entity.Link;
import com.linklens.linkservice.entity.ProcessedEvent;
import com.linklens.linkservice.enums.LinkStatus;
import com.linklens.linkservice.repo.LinkRepository;
import com.linklens.linkservice.repo.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
public class LinkFailureService {

    private final LinkRepository linkRepository;

    private final ProcessedEventRepository processedEventRepository;

    @Transactional
    public void markExtractionFailed(
            UUID eventId,
            UUID linkId,
            String userId,
            String failureCode,
            boolean retryable){

        markFailed(
                eventId,
                "extraction.failed.v1",
                linkId,
                userId,
                failureCode,
                retryable,
                LinkStatus.EXTRACTION_FAILED
        );
    }

    @Transactional
    public void markSummarizationFailed(
            UUID eventId,
            UUID linkId,
            String userId,
            String failureCode,
            boolean retryable){

        markFailed(
                eventId,
                "summarization.failed.v1",
                linkId,
                userId,
                failureCode,
                retryable,
                LinkStatus.SUMMARIZATION_FAILED);
    }

    private void markFailed(
            UUID eventId,
            String eventType,
            UUID linkId,
            String userId,
            String failureCode,
            boolean retryable,
            LinkStatus status){

        if (processedEventRepository.existsById(eventId)){

            log.info(
                    "Duplicate failure event ignored: {}",eventId
            );
            return;
        }

        if (retryable){
            log.info(
                    "Retryable  failure received for link {}. Code: {}",
                    linkId,
                    failureCode);
            return;
        }

        Link link =linkRepository.findById(linkId)
                .orElseThrow(()->
                        new IllegalStateException(
                                "Link not found: "+linkId
                        ));

        if (!link.getUserId().equals(userId)){
            throw new IllegalStateException(
                    "User mismatch for link "+linkId
            );
        }

        if (link.getStatus()!=LinkStatus.COMPLETED){

            link.markFailed(status,failureCode);
        }

        processedEventRepository.save(
                ProcessedEvent.builder()
                        .eventId(eventId)
                        .eventType(eventType)
                        .build()
        );

        log.info(
                "Failure event {} processed for link {}",
                eventId,
                linkId
        );
    }
}
