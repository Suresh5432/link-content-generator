package com.linklens.extractionservice.websiteanalysis.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "website-analysis")
public class WebsiteAnalysisProperties {
    private int maxPages = 12;
    private int maxDepth = 2;
    private int requestTimeoutSeconds = 15;
    private int maxResponseSizeBytes = 2_000_000;
    private int maxRedirects = 5;
    private int concurrency = 2;
    private int overallTimeoutSeconds = 120;
    private long rateLimitMillis = 250;
    private boolean robotsEnabled = true;
}
