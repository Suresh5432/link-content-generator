package com.linklens.summarizationservice.privacyassistant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class WebsiteAnalysisContextClient {

    private final RestClient restClient;
    private final OAuth2AuthorizedClientManager authorizedClientManager;

    public WebsiteAnalysisContextClient(RestClient.Builder builder,
                                        OAuth2AuthorizedClientManager authorizedClientManager,
                                        @Value("${summarization.extraction-service.base-url:http://localhost:8082}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.authorizedClientManager = authorizedClientManager;
    }

    public WebsiteAnalysisSnapshot getContext(String userId) {
        var authorizedClient = authorizedClientManager.authorize(
                OAuth2AuthorizeRequest.withClientRegistrationId("extraction-service")
                        .principal("summarization-service")
                        .build());
        if (authorizedClient == null) {
            throw new IllegalStateException("Unable to obtain Extraction Service access token");
        }
        try {
            WebsiteAnalysisSnapshot snapshot = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/internal/v1/users/{userId}/website-analysis-context")
                            .queryParam("limit", 5)
                            .build(userId))
                    .header("Authorization", "Bearer " + authorizedClient.getAccessToken().getTokenValue())
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        throw new IllegalStateException("Extraction Service denied the website analysis context request");
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                        throw new IllegalStateException("Extraction Service is temporarily unavailable");
                    })
                    .body(WebsiteAnalysisSnapshot.class);
            if (snapshot == null) {
                throw new IllegalStateException("Extraction Service returned an empty website analysis context");
            }
            return snapshot;
        } catch (RuntimeException exception) {
            log.error("Failed to retrieve website analysis context from Extraction Service", exception);
            throw exception;
        }
    }
}
