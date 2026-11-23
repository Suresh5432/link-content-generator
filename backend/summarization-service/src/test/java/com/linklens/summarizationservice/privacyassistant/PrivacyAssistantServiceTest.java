package com.linklens.summarizationservice.privacyassistant;

import com.linklens.summarizationservice.service.ai.AiProvider;
import com.linklens.summarizationservice.service.ai.AiRequest;
import com.linklens.summarizationservice.service.ai.ResponseFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrivacyAssistantServiceTest {

    @Mock AiProvider aiProvider;
    @Mock FootprintContextClient contextClient;
    @Mock WebsiteAnalysisContextClient analysisContextClient;
    @Mock ObjectMapper objectMapper;
    @InjectMocks PrivacyAssistantService service;

    private FootprintSnapshot snapshot() {
        return new FootprintSnapshot(
                List.of(new FootprintSnapshot.Website("Example Bank", "example.com", "finance",
                        "HIGH", "ACTIVE", "EMAIL", "analytics", "NONE", null, null)),
                List.of(new FootprintSnapshot.DataRecord("Example Bank", "BASIC", "EMAIL",
                        "work email", "USER_CONFIRMED", "user", "2026-01-01")),
                List.of(new FootprintSnapshot.RequestRecord("Example Bank", "DELETION", "SUBMITTED",
                        "2026-02-01", "2026-03-01", false, false)),
                List.of("example.com"), 2);
    }

    private WebsiteAnalysisSnapshot analysisSnapshot() {
        return new WebsiteAnalysisSnapshot(List.of(new WebsiteAnalysisSnapshot.Report(
                "a1", "example.com", "https://example.com", "COMPLETED", "HIGH", 80, "FINANCE",
                "Observed tracker usage", List.of(new WebsiteAnalysisSnapshot.Finding("TRACKER", "GA", "analytics", "script", "HIGH", "https://example.com")))));
    }

    @Test
    void promptContainsGroundingRulesAndOwnStoredRecords() {
        FootprintSnapshot snapshot = snapshot();
        WebsiteAnalysisSnapshot analysis = analysisSnapshot();
        when(contextClient.getContext("user-1")).thenReturn(snapshot);
        when(analysisContextClient.getContext("user-1")).thenReturn(analysis);
        when(objectMapper.writeValueAsString(snapshot)).thenReturn("{\"websites\":[{\"name\":\"Example Bank\"}]}");
        when(objectMapper.writeValueAsString(analysis)).thenReturn("{\"reports\":[{\"domain\":\"example.com\"}]}");
        when(aiProvider.generateText(any())).thenReturn("Example Bank has your email (confirmed by you).");

        String answer = service.answer("Which websites have my email?", "user-1");

        assertEquals("Example Bank has your email (confirmed by you).", answer);
        ArgumentCaptor<AiRequest> captor = ArgumentCaptor.forClass(AiRequest.class);
        verify(aiProvider).generateText(captor.capture());
        assertEquals(ResponseFormat.PLAIN_TEXT, captor.getValue().format());
        String prompt = captor.getValue().content();
        assertTrue(prompt.contains("Never invent"));
        assertTrue(prompt.contains("USER_CONFIRMED"));
        assertTrue(prompt.contains("OBSERVED_ON_WEBSITE"));
        assertTrue(prompt.contains("INFERRED_BY_AI"));
        assertTrue(prompt.contains("WEBSITE_ANALYSIS_EVIDENCE"));
        assertTrue(prompt.contains("not proof of account creation"));
        assertTrue(prompt.contains("Never claim that deleting a browser cookie deletes server-side account data"));
        assertTrue(prompt.contains("Example Bank"));
        assertTrue(prompt.contains("Which websites have my email?"));
        verify(contextClient).getContext("user-1");
        verify(analysisContextClient).getContext("user-1");
    }

    @Test
    void blankQuestionIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.answer("   ", "user-1"));
    }

    @Test
    void missingAuthenticatedUserIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.answer("question", " "));
    }

    @Test
    void oversizedQuestionIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.answer("x".repeat(2001), "user-1"));
    }

    @Test
    void rawSecretsAreNotSentToTheProvider() {
        assertThrows(IllegalArgumentException.class,
                () -> service.answer("password: hunter2", "user-1"));
        verifyNoInteractions(contextClient, aiProvider);
    }

    @Test
    void contextLookupFailureIsNotReplacedByAnInventedAnswer() {
        when(contextClient.getContext("user-1")).thenThrow(new IllegalStateException("unavailable"));
        assertThrows(IllegalStateException.class, () -> service.answer("question", "user-1"));
        verifyNoInteractions(aiProvider);
    }


    @Test
    void analysisLookupFailureIsNotReplacedByAnInventedAnswer() {
        when(contextClient.getContext("user-1")).thenReturn(snapshot());
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(analysisContextClient.getContext("user-1")).thenThrow(new IllegalStateException("unavailable"));
        assertThrows(IllegalStateException.class, () -> service.answer("question", "user-1"));
        verify(aiProvider, never()).generateText(any());
    }
}
