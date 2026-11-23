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
public class FootprintContextClient {

    private final RestClient restClient;
    private final OAuth2AuthorizedClientManager authorizedClientManager;

    public FootprintContextClient(RestClient.Builder builder,
                                  OAuth2AuthorizedClientManager authorizedClientManager,
                                  @Value("${summarization.user-service.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.authorizedClientManager = authorizedClientManager;
    }

    public FootprintSnapshot getContext(String userId) {
        var authorizedClient = authorizedClientManager.authorize(
                OAuth2AuthorizeRequest
                        .withClientRegistrationId("user-service")
                        .principal("summarization-service")
                        .build());
        if (authorizedClient == null) {
            throw new IllegalStateException("Unable to obtain User Service access token");
        }
        try {
            FootprintSnapshot snapshot = restClient.get()
                    .uri("/internal/v1/users/{userId}/footprint-context", userId)
                    .header("Authorization", "Bearer " + authorizedClient.getAccessToken().getTokenValue())
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError,
                            (request, response) -> {
                                throw new IllegalStateException("User Service denied the footprint context request");
                            })
                    .onStatus(HttpStatusCode::is5xxServerError,
                            (request, response) -> {
                                throw new IllegalStateException("User Service is temporarily unavailable");
                            })
                    .body(FootprintSnapshot.class);
            if (snapshot == null) {
                throw new IllegalStateException("User Service returned an empty footprint context");
            }
            log.debug("Retrieved footprint context for assistant request");
            return snapshot;
        } catch (RuntimeException exception) {
            log.error("Failed to retrieve footprint context from User Service", exception);
            throw exception;
        }
    }
}
