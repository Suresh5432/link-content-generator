package com.linklens.summarizationservice;

import com.linklens.summarizationservice.service.ai.AiRequest;
import com.linklens.summarizationservice.service.ai.AiResponse;
import com.linklens.summarizationservice.service.ai.MockAiProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MockAiProviderTest {

    private final MockAiProvider provider = new MockAiProvider();

    @Test
    void shouldGenerateMockSummary() {

        AiRequest request = new AiRequest(
                "Understanding Microservices",
                "Microservices are an architectural style. "
                        + "Applications are divided into independent services.",
                "en"
        );

        AiResponse response = provider.generateSummary(request);

        assertNotNull(response);

        assertNotNull(response.summary());
        assertFalse(response.summary().isBlank());

        assertNotNull(response.keyTakeaways());
        assertFalse(response.keyTakeaways().isEmpty());

        assertEquals("MOCK", response.provider());
        assertEquals("mock-v1", response.model());
    }
}
