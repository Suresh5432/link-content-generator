package com.linklens.summarizationservice.privacyassistant.controller;

import com.linklens.summarizationservice.config.SecurityConfig;
import com.linklens.summarizationservice.privacyassistant.PrivacyAssistantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PrivacyAssistantController.class)
@Import(SecurityConfig.class)
class PrivacyAssistantControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean PrivacyAssistantService service;
    @MockitoBean JwtDecoder jwtDecoder;

    @Test
    void anonymousRequestIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/privacy-assistant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"What have I shared?\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void usesAuthenticatedSubjectAndNeverAcceptsClientSuppliedRecords() throws Exception {
        when(service.answer("What have I shared?", "alice")).thenReturn("Grounded answer");
        mockMvc.perform(post("/api/v1/privacy-assistant")
                        .with(jwt().jwt(t -> t.subject("alice")).authorities(() -> "ROLE_USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"What have I shared?\",\"context\":\"faked records\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Grounded answer"));
        verify(service).answer("What have I shared?", "alice");
    }
}
