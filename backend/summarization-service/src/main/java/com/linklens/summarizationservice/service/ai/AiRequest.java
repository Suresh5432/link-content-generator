package com.linklens.summarizationservice.service.ai;

public record AiRequest(
        String title,
        String content,
        String language) {}
