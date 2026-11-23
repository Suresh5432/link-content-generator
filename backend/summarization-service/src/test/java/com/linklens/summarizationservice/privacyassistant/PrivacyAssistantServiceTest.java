package com.linklens.summarizationservice.privacyassistant;

import com.linklens.summarizationservice.service.ai.AiProvider;
import com.linklens.summarizationservice.service.ai.AiRequest;
import com.linklens.summarizationservice.service.ai.AiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrivacyAssistantServiceTest {

    @Mock AiProvider aiProvider;
    @InjectMocks PrivacyAssistantService service;

    @Test
    void promptContainsGroundingAndEvidenceRules() {
        when(aiProvider.generateSummary(any())).thenReturn(new AiResponse("Example Bank has your email (confirmed by you).", List.of(), "mock", "mock", 1, 1));

        String answer = service.answer("Which websites have my email?", "[{\"website\":\"Example Bank\",\"source\":\"USER_CONFIRMED\"}]");

        assertEquals("Example Bank has your email (confirmed by you).", answer);
        ArgumentCaptor<AiRequest> captor = ArgumentCaptor.forClass(AiRequest.class);
        verify(aiProvider).generateSummary(captor.capture());
        String prompt = captor.getValue().content();
        assertTrue(prompt.contains("Never invent"));
        assertTrue(prompt.contains("USER_CONFIRMED"));
        assertTrue(prompt.contains("OBSERVED_ON_WEBSITE"));
        assertTrue(prompt.contains("INFERRED_BY_AI"));
        assertTrue(prompt.contains("Never claim that deleting a browser cookie deletes server-side account data"));
        assertTrue(prompt.contains("Which websites have my email?"));
    }

    @Test
    void blankQuestionIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.answer("   ", "ctx"));
    }

    @Test
    void oversizedContextIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.answer("question", "x".repeat(20001)));
    }
}
