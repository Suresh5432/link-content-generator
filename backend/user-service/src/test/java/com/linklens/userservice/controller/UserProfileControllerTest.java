package com.linklens.userservice.controller;

import com.linklens.userservice.dto.UserProfileResponse;
import com.linklens.userservice.service.UserProfileService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@WebMvcTest(UserProfileController.class)
@Import(UserProfileControllerTest.TestSecurityConfig.class)
class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserProfileService userProfileService;

    @TestConfiguration
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http)
                throws Exception {

            return http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth ->
                            auth.anyRequest().authenticated()
                    )
                    .oauth2ResourceServer(oauth ->
                            oauth.jwt(jwt -> {})
                    )
                    .build();
        }
    }

    private RequestPostProcessor authentication() {
        return jwt().jwt(token -> token.subject("keycloak-user-123"));
    }

    @Test
    void shouldReturnCurrentUserProfile() throws Exception {

        when(userProfileService.getOrCreateProfile("keycloak-user-123", null))
                .thenReturn(new UserProfileResponse(
                        "keycloak-user-123",
                        "Suresh",
                        "katravathsuresh8917@gmail.com",
                        "Java Developer",
                        null,
                        Instant.now(),
                        Instant.now()
                ));

        mockMvc.perform(get("/api/v1/users/me")
                        .with(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId")
                        .value("keycloak-user-123"))
                .andExpect(jsonPath("$.displayName")
                        .value("Suresh"));
        verify(userProfileService).getOrCreateProfile("keycloak-user-123", null);
    }

    @Test
    void synchronizesEmailOnlyWhenVerified() throws Exception {
        mockMvc.perform(get("/api/v1/users/me").with(jwt().jwt(token -> token
                        .subject("subject-id").claim("email", "person@example.org")
                        .claim("email_verified", true))))
                .andExpect(status().isOk());
        verify(userProfileService).getOrCreateProfile("subject-id", "person@example.org");

        clearInvocations(userProfileService);
        mockMvc.perform(get("/api/v1/users/me").with(jwt().jwt(token -> token
                        .subject("subject-id").claim("email", "unverified@example.org")
                        .claim("email_verified", false))))
                .andExpect(status().isOk());
        verify(userProfileService).getOrCreateProfile("subject-id", null);
    }

    @Test
    void shouldRejectUnauthenticatedRequest() throws Exception {

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }
}
