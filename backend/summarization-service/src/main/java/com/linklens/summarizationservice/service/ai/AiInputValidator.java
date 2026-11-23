package com.linklens.summarizationservice.service.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AiInputValidator {

    private final int maxInputCharacters;

    public AiInputValidator(@Value("${summarization.ai.max-input-characters}") int maxInputCharacters) {
        this.maxInputCharacters = maxInputCharacters;
    }

    public AiRequest validate(AiRequest request) {

        if (request==null ||
                request.content()==null ||
                request.content().isBlank()){
            throw new IllegalArgumentException("Content cannot be empty");
        }

        if (request.title() != null && request.title().length() > 1000) {
            throw new IllegalArgumentException("Title exceeds the maximum allowed length");
        }
        if (request.language() != null && request.language().length() > 40) {
            throw new IllegalArgumentException("Language exceeds the maximum allowed length");
        }

        if (request.content().length() <= maxInputCharacters) {
            return request;
        }

        // Web extraction can legitimately return large pages. Bound AI cost and
        // provider limits without turning an otherwise valid link into a retry loop.
        final String marker = "…";
        int contentLimit = Math.max(0, maxInputCharacters - marker.length());
        int safeEnd = Math.min(contentLimit, request.content().length());
        if (safeEnd > 0 && Character.isHighSurrogate(request.content().charAt(safeEnd - 1))) {
            safeEnd--;
        }
        String truncated = request.content().substring(0, safeEnd) + marker;
        return new AiRequest(request.title(), truncated, request.language());
    }
}
