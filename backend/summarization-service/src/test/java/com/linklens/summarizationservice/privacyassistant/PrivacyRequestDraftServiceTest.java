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
class PrivacyRequestDraftServiceTest {

    @Mock AiProvider aiProvider;
    @InjectMocks PrivacyRequestDraftService service;

    @Test
    void promptKeepsDraftGroundedAndFreeOfSecrets() {
        when(aiProvider.generateSummary(any())).thenReturn(new AiResponse("Subject: Request for Deletion of Personal Data\n\nHello...", List.of(), "mock", "mock", 1, 1));

        String draft = service.draft("DELETION", "Example Bank", "email, address");

        assertTrue(draft.startsWith("Subject:"));
        ArgumentCaptor<AiRequest> captor = ArgumentCaptor.forClass(AiRequest.class);
        verify(aiProvider).generateSummary(captor.capture());
        String prompt = captor.getValue().content();
        assertTrue(prompt.contains("Return ONLY the finished message"));
        assertTrue(prompt.contains("Never invent account identifiers"));
        assertTrue(prompt.contains("Never include passwords"));
        assertTrue(prompt.contains("identity-verification steps"));
        assertTrue(prompt.contains("Example Bank"));
        assertTrue(prompt.contains("email, address"));
    }

    @Test
    void emptyProviderOutputIsRejected() {
        when(aiProvider.generateSummary(any())).thenReturn(new AiResponse("  ", List.of(), "mock", "mock", 1, 1));
        assertThrows(IllegalStateException.class, () -> service.draft("ACCESS", "Example", null));
    }
}
