package com.linklens.summarizationservice.service.ai;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Component
@Slf4j
@ConditionalOnProperty(
        name = "summarization.ai.provider",
        havingValue = "gemini"
)
public class GeminiAiProvider implements AiProvider {

    private final Client client;

    private final ObjectMapper objectMapper;

    private final String model;

    public GeminiAiProvider(ObjectMapper objectMapper,
                            @Value("${summarization.ai.model}") String model) {
        this.client = new Client();
        this.objectMapper = objectMapper;
        this.model = model;
    }
    @Override
    public AiResponse generateSummary(AiRequest request) {
        String responseText = execute(request);

        try {
            GeminiSummaryOutput output=
                    objectMapper.readValue(
                            responseText,
                            GeminiSummaryOutput.class
                    );

            if (output == null || output.summary() == null || output.summary().isBlank()) {
                throw new IllegalStateException("Gemini returned an empty summary");
            }

            return new AiResponse(
                    output.summary(),
                    output.keyTakeaways(),
                    getProviderName(),
                    getModelName(),
                    null,
                    null
            );
        }
        catch (Exception exception){
            log.error("Gemini summarization response parsing failed for model {}", model, exception);
            throw new IllegalStateException("Gemini summarization failed", exception);
        }
    }

    @Override
    public String generateText(AiRequest request) {
        String responseText = execute(request);
        if (responseText.isBlank()) {
            throw new IllegalStateException("Gemini returned an empty response");
        }
        return responseText;
    }

    private String execute(AiRequest request) {
        String prompt = request.format() == ResponseFormat.PLAIN_TEXT
                ? AiPrompts.plainTextPrompt(request)
                : AiPrompts.summaryPrompt(request);
        try{
            GenerateContentResponse response=
                    client.models.generateContent(model,prompt,null);
            log.debug("Received Gemini response for model {}", model);

            String responseText=response.text();
            if (responseText==null || responseText.isBlank()){
                throw new IllegalStateException("Gemini returned an empty response");
            }

            return responseText.trim()
                    .replaceFirst("^```json\\s*", "")
                    .replaceFirst("^```\\s*", "")
                    .replaceFirst("\\s*```$", "");
        }
        catch (Exception exception){
            log.error("Gemini request failed for model {}", model, exception);
            throw new IllegalStateException("Gemini request failed", exception);
        }
    }

    @Override
    public String getProviderName() {
        return "GEMINI";
    }

    @Override
    public String getModelName() {
        return model;
    }

    private record GeminiSummaryOutput(
            String summary,
            List<String> keyTakeaways
    ){}
}
