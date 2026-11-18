package com.linklens.extractionservice.service;

import com.linklens.extractionservice.dto.ExtractedPage;
import com.linklens.extractionservice.dto.ExtractionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExtractionService {

    private final SafeWebFetcher safeWebFetcher;
    private final ContentExtractor contentExtractor;

    public ExtractionResult extract(String url){

        log.info("Starting safe webpage fetch: {}", url);

        SafeWebFetcher.FetchedPage fetchedPage = safeWebFetcher.fetch(url);

        log.info("Webpage fetched successfully. Final URL: {}, Content-Type: {}",
                fetchedPage.finalUrl(),
                fetchedPage.contentType());

        ExtractedPage extractedPage=contentExtractor.extract(fetchedPage);

        log.info("Content extraction completed. Title: {}, Word count: {}",
                extractedPage.getTitle(),
                extractedPage.getWordCount());

        return  ExtractionResult.builder()
                .finalUrl(fetchedPage.finalUrl())
                .responseContentType(fetchedPage.contentType())
                .page(extractedPage)
                .build();
    }

}
