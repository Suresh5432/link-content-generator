package com.linklens.linkservice.service;

import com.linkcontext.contracts.events.SummaryGeneratedEvent;
import com.linklens.linkservice.entity.Link;
import com.linklens.linkservice.enums.LinkStatus;
import com.linklens.linkservice.repo.LinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Slf4j
@RequiredArgsConstructor
public class SummaryGeneratedService {

    private final LinkRepository linkRepository;

    @Transactional
    public void process(SummaryGeneratedEvent event){
        Link link=linkRepository.findById(event.linkId())
                .orElseThrow(()->
                        new IllegalStateException(
                                "Link not found: " +event.linkId()
                        )
                );

        if (!link.getUserId().equals(event.userId())){
            throw new IllegalStateException(
                    "User ID mismatch for link: "+event.linkId()
            );
        }

        if (link.getStatus()== LinkStatus.COMPLETED){

            log.info("Link {} is already completed, Ignoring duplicate event {}",
                    event.linkId(),event.eventId());

            return;
        }

        link.updateStatus(LinkStatus.COMPLETED);

        linkRepository.save(link);

        log.info("Link {} successfully completed. Summary event: {}",event.linkId(),event.eventId());
    }
}
