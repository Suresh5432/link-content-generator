package com.linklens.linkservice.service;

import com.linklens.linkservice.dto.LinkRequest;
import com.linklens.linkservice.dto.LinkResponse;
import com.linklens.linkservice.entity.Link;
import com.linklens.linkservice.enums.LinkStatus;
import com.linklens.linkservice.repo.LinkRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LinkService {

    private final LinkRepository linkRepository;

    @Transactional
    public LinkResponse createLink(LinkRequest linkRequest, String userId) {
        String normalizedUrl=linkRequest.getUrl().trim();
        if(!normalizedUrl.startsWith("http://")&&
                !normalizedUrl.startsWith("https://")) {
            throw new IllegalArgumentException("Only HTTP and HTTPS URLs are supported");
        }

        Link link=Link.builder()
                .url(normalizedUrl)
                .userId(userId)
                .linkStatus(LinkStatus.PENDING_EXTRACTION)
                .build();
        Link savedLink=linkRepository.save(link);
        return LinkResponse.builder()
                .linkId(savedLink.getId().toString())
                .status(savedLink.getLinkStatus())
                .message("Link submitted successfully")
                .createdAt(savedLink.getCreatedAt())
                .build();
    }
}
