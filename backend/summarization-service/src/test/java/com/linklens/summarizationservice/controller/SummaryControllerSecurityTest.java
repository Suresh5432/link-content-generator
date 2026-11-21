package com.linklens.summarizationservice.controller;

import com.linklens.summarizationservice.config.SecurityConfig;
import com.linklens.summarizationservice.dto.SummaryResponse;
import com.linklens.summarizationservice.dto.PagedSummaryResponse;
import com.linklens.summarizationservice.service.SummaryQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SummaryController.class)
@Import(SecurityConfig.class)
class SummaryControllerSecurityTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    SummaryQueryService summaryQueryService;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Test
    void rejectsAnonymousSummaryRequest() throws Exception {
        mockMvc.perform(get("/api/v1/summaries"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(summaryQueryService);
    }

    @Test
    void passesJwtSubjectToOwnerScopedListing() throws Exception {
        UUID id = UUID.randomUUID();
        SummaryResponse response = new SummaryResponse(id, UUID.randomUUID(), UUID.randomUUID(),
                "Title", "Summary", List.of("Point"), 1, 20, "ollama", "qwen2.5:3b",
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"));
        when(summaryQueryService.list("alice", 0, 20, "createdAt", Sort.Direction.DESC))
                .thenReturn(PagedSummaryResponse.from(new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1)));

        mockMvc.perform(get("/api/v1/summaries")
                        .with(jwt().jwt(token -> token.subject("alice")).authorities(() -> "ROLE_USER"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id.toString()));

        verify(summaryQueryService).list("alice", 0, 20, "createdAt", Sort.Direction.DESC);
    }

    @Test
    void rejectsAuthenticatedTokenWithoutUserRole() throws Exception {
        mockMvc.perform(get("/api/v1/summaries")
                        .with(jwt().jwt(token -> token.subject("alice"))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(summaryQueryService);
    }
}
