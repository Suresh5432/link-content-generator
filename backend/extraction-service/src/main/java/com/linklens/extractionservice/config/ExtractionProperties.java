package com.linklens.extractionservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "extraction.fetch")
@Getter
@Setter
public class ExtractionProperties {

    private int connectTimeoutSeconds=5;

    private int requestTimeoutSeconds=15;

    private int maxResponseSizeBytes=5*1024*1024;

    private int maxRedirects=5;

    private String userAgent="LinkContextBot/1.0";

}
