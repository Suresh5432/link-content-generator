package com.linklens.extractionservice.websiteanalysis.config;

import com.linklens.extractionservice.config.ExtractionProperties;
import com.linklens.extractionservice.service.SafeWebFetcher;
import com.linklens.extractionservice.service.UrlSafetyValidator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebsiteAnalysisFetcherConfig {

    @Bean
    public ExtractionProperties websiteAnalysisExtractionProperties(WebsiteAnalysisProperties props) {
        ExtractionProperties p = new ExtractionProperties();
        p.setConnectTimeoutSeconds(props.getRequestTimeoutSeconds());
        p.setRequestTimeoutSeconds(props.getRequestTimeoutSeconds());
        p.setMaxResponseSizeBytes(props.getMaxResponseSizeBytes());
        p.setMaxRedirects(props.getMaxRedirects());
        return p;
    }

    @Bean
    public SafeWebFetcher websiteAnalysisFetcher(
            UrlSafetyValidator validator,
            @Qualifier("websiteAnalysisExtractionProperties") ExtractionProperties websiteAnalysisExtractionProperties) {
        return new SafeWebFetcher(validator, websiteAnalysisExtractionProperties);
    }
}
