package com.linklens.summarizationservice.service.ai;

import org.springframework.stereotype.Component;

@Component
public class AiResponseValidator {

    private static final int MAX_SUMMARY_LENGTH= 10_000;

    private static final int MAX_TAKEAWAYS=10;

    private static final int MAX_TAKEAWAY_LENGTH=500;

    public AiResponse validate(AiResponse response) {
        if (response==null){
            throw new IllegalArgumentException("AI provider returned an empty response");
        }

        if (response.summary()==null || response.summary().isBlank()){
            throw new IllegalArgumentException("AI provider returned an empty summary");
        }

        if (response.summary().length()>MAX_SUMMARY_LENGTH){
            throw new IllegalArgumentException("AI summary exceeds maximum allowed length");
        }

        if (response.keyTakeaways()==null || response.keyTakeaways().isEmpty()){
            throw new IllegalArgumentException("AI must generate at least one key takeaway");
        }
        if (response.keyTakeaways().size() > MAX_TAKEAWAYS) {
            throw new IllegalArgumentException("AI returned too many key takeaways");
        }
        if (response.provider() == null || response.provider().isBlank()
                || response.model() == null || response.model().isBlank()) {
            throw new IllegalArgumentException("AI provider and model metadata are required");
        }

        for (String takeaway : response.keyTakeaways()){

            if (takeaway==null || takeaway.isBlank()){
                throw new IllegalArgumentException("Key takeaways cannot be empty");
            }

            if (takeaway.length()>MAX_TAKEAWAY_LENGTH){
                throw new IllegalArgumentException("A Key takeaways exceeds maximum allowed length");
            }
        }
        return response;
    }


}
