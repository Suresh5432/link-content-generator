package com.linklens.summarizationservice.service.ai;

public record AiRequest(
        String title,
        String content,
        String language,
        ResponseFormat format) {

    public AiRequest(String title, String content, String language) {
        this(title, content, language, ResponseFormat.SUMMARY_JSON);
    }

    public ResponseFormat format() {
        return format == null ? ResponseFormat.SUMMARY_JSON : format;
    }
}
