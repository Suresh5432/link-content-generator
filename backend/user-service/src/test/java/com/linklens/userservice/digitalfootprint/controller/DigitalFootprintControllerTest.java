package com.linklens.userservice.digitalfootprint.controller;

import com.linklens.userservice.digitalfootprint.entity.TrackedWebsite;
import com.linklens.userservice.digitalfootprint.service.DigitalFootprintService;
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

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DigitalFootprintController.class)
@Import(DigitalFootprintControllerTest.TestSecurityConfig.class)
class DigitalFootprintControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean DigitalFootprintService service;

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
    void anonymousRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/digital-footprint/websites/metrics")).andExpect(status().isUnauthorized());
    }

    @Test
    void metricsUseAuthenticatedSubject() throws Exception {
        when(service.metrics("user-1")).thenReturn(Map.of("totalWebsites", 3, "outstandingActions", 1));
        mockMvc.perform(get("/api/v1/digital-footprint/websites/metrics").with(jwt().jwt(t -> t.subject("user-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalWebsites").value(3));
        verify(service).metrics("user-1");
    }

    @Test
    void createUsesAuthenticatedSubjectAndValidatesInput() throws Exception {
        TrackedWebsite website = TrackedWebsite.builder().id(UUID.randomUUID()).userId("user-1").name("Example Bank").domain("example.com").url("https://example.com").build();
        when(service.create(eq("user-1"), org.mockito.ArgumentMatchers.any())).thenReturn(website);
        mockMvc.perform(post("/api/v1/digital-footprint/websites")
                        .with(jwt().jwt(t -> t.subject("user-1")))
                        .contentType("application/json")
                        .content("{\"name\":\"Example Bank\",\"domain\":\"example.com\",\"url\":\"https://example.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.domain").value("example.com"));
        verify(service).create(eq("user-1"), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createRejectsMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/v1/digital-footprint/websites")
                        .with(jwt().jwt(t -> t.subject("user-1")))
                        .contentType("application/json")
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
