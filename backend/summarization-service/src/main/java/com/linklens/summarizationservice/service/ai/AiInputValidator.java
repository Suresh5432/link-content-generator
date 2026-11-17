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

        if (request.content().length()>maxInputCharacters){
            throw new IllegalArgumentException("Content exceeds the maximum AI input size");
        }

        return request;
    }
}
