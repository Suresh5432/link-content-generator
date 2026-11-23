package com.linklens.summarizationservice.privacyassistant;

import java.util.List;

public record WebsiteAnalysisSnapshot(
        List<Report> reports) {

    public record Report(
            String analysisId,
            String domain,
            String url,
            String status,
            String riskLevel,
            Integer confidence,
            String category,
            String summary,
            List<Finding> findings) {}

    public record Finding(
            String type,
            String name,
            String description,
            String evidence,
            String confidence,
            String pageUrl) {}
}
