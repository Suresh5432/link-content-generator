package com.linklens.summarizationservice.service;

import com.google.common.net.HttpHeaders;
import com.linklens.summarizationservice.dto.ExtractedContentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class ExtractionContentClient {

    private final RestClient restClient;

    private final OAuth2AuthorizedClientManager authorizedClientManager;

    public ExtractionContentClient(
            RestClient.Builder builder,
            OAuth2AuthorizedClientManager authorizedClientManager,
            @Value("${summarization.content-service.base-url}")
            String baseUrl){
        this.restClient = builder
                .baseUrl(baseUrl)
                .build();
        this.authorizedClientManager = authorizedClientManager;
    }

    public ExtractedContentResponse getContent(UUID contentId){

        var authorizedClient=authorizedClientManager.authorize(
                OAuth2AuthorizeRequest
                        .withClientRegistrationId("extraction-service")
                        .principal("summarization-service")
                        .build()
        );

        if (authorizedClient == null){
            throw new IllegalStateException("Unable to obtain Extraction Service access token");
        }

        String accessToken=authorizedClient.getAccessToken()
                .getTokenValue();

        return restClient.get()
                .uri("/internal/v1/contents/{contentId}",contentId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer "+accessToken)
                .retrieve()
                .onStatus(HttpStatusCode::is5xxServerError,
                        (request, response) -> {
                                throw new IllegalStateException(
                                    "Extraction Service is temporarily unavailable"
                                );
                        })
                .body(ExtractedContentResponse.class);
    }

}
