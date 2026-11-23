package com.linklens.summarizationservice.service.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Component
@Slf4j
@ConditionalOnProperty(
        name = "summarization.ai.provider",
        havingValue = "ollama",
        matchIfMissing = true
)
public class OllamaAiProvider implements AiProvider {

    private final RestClient restClient;

    private final ObjectMapper objectMapper;

    private final String model;

    public OllamaAiProvider(RestClient.Builder restClient,
                            ObjectMapper objectMapper,
                            @Value("${summarization.ollama.base-url}")
                            String baseUrl,
                            @Value("${summarization.ai.model}")
                            String model) {
        this.restClient=restClient
                .baseUrl(baseUrl)
                .build();

        this.objectMapper=objectMapper;

        this.model=model;
    }

    @Override
    public AiResponse generateSummary(AiRequest request) {
        OllamaResponse response = execute(request, "json");
        try {
            SummaryOutput output=objectMapper.readValue(
                    response.response(),
                    SummaryOutput.class
            );

            if (output == null || output.summary() == null || output.summary().isBlank()) {
                throw new IllegalStateException("Ollama returned an empty summary");
            }

            return new AiResponse(
                    output.summary(),
                    output.keyTakeaways(),
                    getProviderName(),
                    getModelName(),
                    response.promptEvalCount(),
                    response.evalCount()
            );
        }
        catch (Exception ex){
            log.error("Unable to parse Ollama response for model {}", model, ex);
            throw new IllegalStateException(
                    "Unable to parse Ollama summarization response", ex
            );
        }
    }

    @Override
    public String generateText(AiRequest request) {
        return execute(request, null).response().trim();
    }

    private OllamaResponse execute(AiRequest request, String format) {
        String prompt = request.format() == ResponseFormat.PLAIN_TEXT
                ? AiPrompts.plainTextPrompt(request)
                : AiPrompts.summaryPrompt(request);

        OllamaRequest ollamaRequest =
                new OllamaRequest(model, prompt, false, format, new OllamaOptions(0.2));

        try{
            OllamaResponse response=restClient.post()
                    .uri("/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ollamaRequest)
                    .retrieve()
                    .body(OllamaResponse.class);
            log.debug("Received Ollama response for model {}", model);

            if (response==null ||
                    response.response()==null ||
                    response.response().isBlank()){
                throw new IllegalStateException(
                        "Ollama returned an empty response"
                );
            }
            return response;
        }
        catch (RestClientException exception){
            log.error("Unable to communicate with Ollama for model {}", model, exception);
            throw new IllegalStateException(
                    "Unable to communicate with Ollama", exception
            );
        }
    }

    @Override
    public String getProviderName() {
        return "OLLAMA";
    }

    @Override
    public String getModelName() {
        return model;
    }

    private  record OllamaRequest(
            String model,
            String prompt,
            boolean stream,
            String format,
            OllamaOptions options
    ){}

    private record OllamaOptions(
            double temperature
    ){}

    private record OllamaResponse(
            String response,
            Integer promptEvalCount,
            Integer evalCount
    ){}

    private record SummaryOutput(
            String summary,
            List<String> keyTakeaways
    ){}
}
