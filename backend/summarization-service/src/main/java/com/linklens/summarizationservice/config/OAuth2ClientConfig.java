package com.linklens.summarizationservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestClient;

@Configuration
public class OAuth2ClientConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth->
                        auth.requestMatchers("/actuator/health",
                                        "/actuator/health/**",
                                        "/actuator/info").permitAll()
                                .anyRequest().permitAll()
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .oauth2Login(AbstractHttpConfigurer::disable)
                .build();
    }

    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService auth2AuthorizedClientService){

        OAuth2AuthorizedClientProvider provider =
                OAuth2AuthorizedClientProviderBuilder
                        .builder()
                        .clientCredentials()
                        .build();
        var manager=new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                clientRegistrationRepository,
                auth2AuthorizedClientService);

        manager.setAuthorizedClientProvider(provider);
        return manager;

    }

    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

}
