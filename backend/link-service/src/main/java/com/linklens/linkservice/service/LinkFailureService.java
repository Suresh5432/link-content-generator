package com.linklens.linkservice.service;

import com.linklens.linkservice.entity.Link;
import com.linklens.linkservice.enums.LinkStatus;
import com.linklens.linkservice.repo.LinkRepository;
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

    @Transactional
    public void markExtractionFailed(
            UUID linkId,
            String userId,
            String failureCode){

        markFailed(
                linkId,
                userId,
                failureCode,
                LinkStatus.EXTRACTION_FAILED
        );
    }

    @Transactional
    public void markSummarizationFailed(
            UUID linkId,
            String userId,
            String failureCode){
        markFailed(
                linkId,
                userId,
                failureCode,
                LinkStatus.SUMMARIZATION_FAILED);
    }

    private void markFailed(
            UUID linkId,
            String userId,
            String failureCode,
            LinkStatus status){

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

        if (link.getStatus()==LinkStatus.COMPLETED){

            log.warn(
                    "Ignoring failure event for completed link {}",linkId

            );

            link.markFailed(status, failureCode);

            log.warn("Link {} marked as {}", linkId, status);
        }
    }
}
