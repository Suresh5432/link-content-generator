package com.linklens.userservice.digitalfootprint.controller;

import com.linklens.userservice.digitalfootprint.dto.FootprintContextResponse;
import com.linklens.userservice.digitalfootprint.service.FootprintContextService;
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
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InternalFootprintContextController.class)
@Import(InternalFootprintContextControllerTest.TestSecurityConfig.class)
@TestPropertySource(properties = "security.internal-client-ids=summarization-service")
class InternalFootprintContextControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean FootprintContextService contextService;

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
    void authorizedServiceClientCanReadContext() throws Exception {
        when(contextService.buildContext("user-1")).thenReturn(new FootprintContextResponse(
                List.of(), List.of(), List.of(), List.of(), 0));
        mockMvc.perform(get("/internal/v1/users/user-1/footprint-context")
                        .with(jwt().jwt(t -> t.claim("azp", "summarization-service"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingCookieCount").value(0));
        verify(contextService).buildContext("user-1");
    }

    @Test
    void unauthorizedServiceClientIsForbidden() throws Exception {
        mockMvc.perform(get("/internal/v1/users/user-1/footprint-context")
                        .with(jwt().jwt(t -> t.claim("azp", "attacker-client"))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(contextService);
    }

    @Test
    void anonymousRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get("/internal/v1/users/user-1/footprint-context"))
                .andExpect(status().isUnauthorized());
    }
}
