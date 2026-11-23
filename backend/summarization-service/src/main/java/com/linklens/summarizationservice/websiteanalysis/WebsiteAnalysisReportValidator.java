package com.linklens.summarizationservice.websiteanalysis;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Component
public class WebsiteAnalysisReportValidator {

    private static final int MAX_REPORT_LENGTH = 100_000;
    private static final List<String> REQUIRED_FIELDS = List.of(
            "overview", "purpose", "functionality", "requestedInformation",
            "sensitiveInformation", "trackers", "privacy", "security",
            "trustAssessment", "limitations", "recommendedPrecautions"
    );

    private final ObjectMapper objectMapper;

    public WebsiteAnalysisReportValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void validate(String report) {
        if (report == null || report.isBlank() || report.length() > MAX_REPORT_LENGTH) {
            throw new IllegalStateException("AI report is empty or exceeds the size limit");
        }
        JsonNode node;
        try {
            node = objectMapper.readTree(report);
        } catch (RuntimeException e) {
            throw new IllegalStateException("AI report is not valid JSON", e);
        }
        if (node == null || !node.isObject()) {
            throw new IllegalStateException("AI report must be a JSON object");
        }
        for (String field : REQUIRED_FIELDS) {
            JsonNode value = node.get(field);
            if (value == null || value.isNull()
                    || (!value.isObject() && !value.isArray() && !value.isString())) {
                throw new IllegalStateException("AI report is missing required field: " + field);
            }
        }
    }
}
