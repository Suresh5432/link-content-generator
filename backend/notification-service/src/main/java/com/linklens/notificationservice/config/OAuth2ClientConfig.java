package com.linklens.notificationservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.client.RestClient;

@Configuration
public class OAuth2ClientConfig {

    @Bean
    public RestClient userServiceRestClient(
            RestClient.Builder builder,
            OAuth2AuthorizedClientManager auth2AuthorizedClientManager,
            @Value("${USER_SERVICE_BASE_URL:http://localhost:8085}") String baseUrl){

        return builder
                .baseUrl(baseUrl)
                .requestInterceptor(((request, body, execution) -> {
                    var authorizedClient = auth2AuthorizedClientManager.authorize(
                            OAuth2AuthorizeRequest
                                    .withClientRegistrationId("user-service")
                                    .principal("notification-service")
                                    .build()
                    );

                    if (authorizedClient == null){
                        throw new IllegalStateException(
                                "Unable to obtain User Service access token");
                    }

                    request.getHeaders()
                            .setBearerAuth(
                                    authorizedClient
                                            .getAccessToken()
                                            .getTokenValue());

                    return execution.execute(request, body);
                }))
                .build();
    }

    @Bean
    public RestClient.Builder restClient(){
        return RestClient.builder();
    }

    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository registrations,
            OAuth2AuthorizedClientService auth2AuthorizedClientService){

        var provider= OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build();

        var manager=new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                registrations,
                auth2AuthorizedClientService
        );

        manager.setAuthorizedClientProvider(provider);

        return manager;
    }
}
