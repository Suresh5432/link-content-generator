package com.linklens.userservice.digitalfootprint.controller;

import com.linklens.userservice.digitalfootprint.entity.PrivacyRequest;
import com.linklens.userservice.digitalfootprint.service.PrivacyRequestService;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PrivacyRequestController.class)
@Import(PrivacyRequestControllerTest.TestSecurityConfig.class)
class PrivacyRequestControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean PrivacyRequestService service;

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

    private PrivacyRequest request(String status) {
        return PrivacyRequest.builder().id(UUID.randomUUID()).userId("user-1").requestType("DELETION").status(status).requestMessage("Please delete my data.").build();
    }

    @Test
    void anonymousRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/privacy-requests")).andExpect(status().isUnauthorized());
    }

    @Test
    void listUsesAuthenticatedSubject() throws Exception {
        when(service.list("user-1")).thenReturn(List.of(request("DRAFT")));
        mockMvc.perform(get("/api/v1/privacy-requests").with(jwt().jwt(t -> t.subject("user-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requestType").value("DELETION"))
                .andExpect(jsonPath("$[0].status").value("DRAFT"));
        verify(service).list("user-1");
    }

    @Test
    void approveUsesAuthenticatedSubject() throws Exception {
        PrivacyRequest approved = request("AWAITING_USER_APPROVAL");
        when(service.approve(eq("user-1"), any())).thenReturn(approved);
        mockMvc.perform(post("/api/v1/privacy-requests/{id}/approve", approved.getId()).with(jwt().jwt(t -> t.subject("user-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AWAITING_USER_APPROVAL"));
        verify(service).approve("user-1", approved.getId());
    }

    @Test
    void submitPassesUserApprovedSubmissionMethod() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.submit(eq("user-1"), eq(id), eq("EMAIL"), eq("ref-9"))).thenReturn(request("SUBMITTED"));
        mockMvc.perform(post("/api/v1/privacy-requests/{id}/submit", id)
                        .with(jwt().jwt(t -> t.subject("user-1")))
                        .contentType("application/json")
                        .content("{\"submissionMethod\":\"EMAIL\",\"externalReference\":\"ref-9\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
        verify(service).submit("user-1", id, "EMAIL", "ref-9");
    }
}
