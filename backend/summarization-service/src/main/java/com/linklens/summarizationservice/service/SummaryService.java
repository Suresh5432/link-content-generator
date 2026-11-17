package com.linklens.summarizationservice.service;

import com.linklens.summarizationservice.dto.ExtractedContentResponse;
import com.linklens.summarizationservice.dto.SummaryResult;
import com.linklens.summarizationservice.service.ai.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SummaryService {

    private final ExtractionContentClient contentClient;

    private final AiProvider aiProvider;

    private final AiInputValidator aiInputValidator;

    private final AiResponseValidator aiResponseValidator;

    private final ReadingTimeCalculator readingTimeCalculator;

    private final SummaryPersistenceService persistenceService;

    public void generateSummary(
            UUID sourceEventId,
            UUID linkId,
            UUID contentId,
            String userId,
            String title,
            String language){

        ExtractedContentResponse content=
                contentClient.getContent(contentId);

        if (content==null ||
                content.extractedText()==null||
                content.extractedText().isBlank()){
            throw new IllegalArgumentException(
                    "Extracted webpage content is empty");
        }

        if (!contentId.equals(content.contentId())){
            throw new IllegalStateException(
                    "Content Id mismatch"
            );
        }

        if (!content.linkId().equals(linkId)||
                !content.userId().equals(userId)){

            throw new IllegalStateException(
                    "Retrieved content does not match the incoming event");
        }

        AiRequest request=aiInputValidator.validate(
                new AiRequest(
                        title !=null ? title :content.title(),
                        content.extractedText(),
                        language !=null ? language :content.language()
                )
        );

        AiResponse response=aiResponseValidator.validate(
                aiProvider.generateSummary(request)
        );

        int readingTime=readingTimeCalculator.calculate(content.extractedText());

        SummaryResult result=new SummaryResult(
                response.summary(),
                response.keyTakeaways(),
                readingTime,
                content.wordCount(),
                response.provider(),
                response.model()
        );

        persistenceService.save(sourceEventId,
                linkId,
                contentId,
                userId,
                request.title(),
                result);

    }

}
