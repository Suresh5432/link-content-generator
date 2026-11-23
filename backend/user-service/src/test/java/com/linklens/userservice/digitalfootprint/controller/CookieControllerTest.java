package com.linklens.userservice.digitalfootprint.controller;

import com.linklens.userservice.digitalfootprint.service.CookieInventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CookieController.class)
@Import(CookieControllerTest.TestSecurityConfig.class)
class CookieControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean CookieInventoryService service;

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http.csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> {}))
                    .build();
        }
    }

    @Test
    void anonymousCookieAccessIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/cookies")).andExpect(status().isUnauthorized());
    }

    @Test
    void deletingMetadataExplainsBrowserRemoval() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.deleteMetadata("user-1", id)).thenReturn("Cookie metadata removed. The cookie itself remains in your browser until you remove it.");
        mockMvc.perform(delete("/api/v1/cookies/inventory/{id}", id).with(jwt().jwt(t -> t.subject("user-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("remains in your browser")));
        verify(service).deleteMetadata("user-1", id);
    }
}
