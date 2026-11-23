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
        when(aiProvider.generateText(any())).thenReturn("Subject: Request for Deletion of Personal Data\n\nHello...");

        String draft = service.draft("DELETION", "Example Bank", "email, address");

        assertTrue(draft.startsWith("Subject:"));
        ArgumentCaptor<AiRequest> captor = ArgumentCaptor.forClass(AiRequest.class);
        verify(aiProvider).generateText(captor.capture());
        assertEquals(ResponseFormat.PLAIN_TEXT, captor.getValue().format());
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
        when(aiProvider.generateText(any())).thenReturn("  ");
        assertThrows(IllegalStateException.class, () -> service.draft("ACCESS", "Example", null));
    }

    @Test
    void rawSecretsAreNotSentToTheProvider() {
        assertThrows(IllegalArgumentException.class,
                () -> service.draft("DELETION", "Example", "card number: 4111111111111111"));
    }
}
