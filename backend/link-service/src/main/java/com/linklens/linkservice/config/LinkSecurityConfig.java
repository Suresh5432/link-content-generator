package com.linklens.linkservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;



@Configuration
@EnableMethodSecurity
public class LinkSecurityConfig {


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session->
                        session
                                .sessionCreationPolicy(
                                        SessionCreationPolicy.STATELESS
                                )
                )
                .authorizeHttpRequests(auth->
                        auth
                                .requestMatchers("/actuator/health",
                                        "/actuator/health/**",
                                        "/actuator/info",
                                        "/actuator/prometheus"
                                ).permitAll()
                                .requestMatchers("/api/v1/**").authenticated()
                                .anyRequest().authenticated()
                        )
                .oauth2ResourceServer(oauth->
                        oauth
                                .jwt(jwt->
                                        jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .build();
    }

    @Bean
    public JwtDecoder jwtDecoder(

            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
            String issuerUri,
            @Value("${KEYCLOAK_EXPECTED_AUDIENCE}")
            String expectedAudience
    ){
        NimbusJwtDecoder decoder=NimbusJwtDecoder
                .withIssuerLocation(issuerUri).build();

        OAuth2TokenValidator<Jwt> issuerValidator=
                JwtValidators.createDefaultWithIssuer(issuerUri);

        OAuth2TokenValidator<Jwt> audienceValidator=
                new AudienceValidator(expectedAudience);

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                issuerValidator,audienceValidator));

        return decoder;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter(){

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(
                new KeycloakRoleConverter());

        converter.setPrincipalClaimName("sub");
        return converter;
    }

    @Bean
    public ObjectMapper objectMapper(){
        return new ObjectMapper();
    }

}
