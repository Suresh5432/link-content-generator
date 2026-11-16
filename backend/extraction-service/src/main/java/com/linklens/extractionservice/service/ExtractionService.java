package com.linklens.extractionservice.service;

import com.linklens.extractionservice.dto.ExtractedPage;
import com.linklens.extractionservice.dto.ExtractionResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExtractionService {

    private final SafeWebFetcher safeWebFetcher;
    private final ContentExtractor contentExtractor;

    public ExtractionResult extract(String url){

        SafeWebFetcher.FetchedPage fetchedPage = safeWebFetcher.fetch(url);

        ExtractedPage extractedPage=contentExtractor.extract(fetchedPage);

        return  ExtractionResult.builder()
                .finalUrl(fetchedPage.finalUrl())
                .responseContentType(fetchedPage.contentType())
                .page(extractedPage)
                .build();
    }

}
