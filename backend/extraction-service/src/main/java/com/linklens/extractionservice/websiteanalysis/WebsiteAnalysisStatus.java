package com.linklens.extractionservice.websiteanalysis;

public enum WebsiteAnalysisStatus {
    QUEUED,
    CRAWLING,
    EXTRACTING,
    ANALYZING,
    COMPLETED,
    PARTIALLY_COMPLETED,
    FAILED
}
