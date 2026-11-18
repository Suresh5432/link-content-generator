package com.linklens.extractionservice.service;

import com.linklens.extractionservice.dto.ExtractedContentResponse;
import com.linklens.extractionservice.entity.ExtractionContent;
import com.linklens.extractionservice.repo.ExtractionContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InternalContentService {

    private final ExtractionContentRepository extractionContentRepository;

    @Transactional(readOnly = true)
    public ExtractedContentResponse getContent(UUID contentId){
        ExtractionContent content =
                extractionContentRepository
                        .findById(contentId)
                        .orElseThrow(()-> new IllegalArgumentException(
                                "Extracted content not found: "+contentId
                        ));

        return new ExtractedContentResponse(
                content.getId(),
                content.getLinkId(),
                content.getUserId(),
                content.getSourceUrl(),
                content.getTitle(),
                content.getDescription(),
                content.getLanguage(),
                content.getExtractedText(),
                content.getWordCount()
        );
    }
}
