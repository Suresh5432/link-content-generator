package com.linklens.summarizationservice;

import com.linklens.summarizationservice.service.ai.AiResponse;
import com.linklens.summarizationservice.service.ai.AiResponseValidator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class AiResponseValidatorTest {
    private final AiResponseValidator validator =
            new AiResponseValidator();

    @Test
    void shouldAcceptValidResponse() {

        AiResponse response = new AiResponse(
                "Microservices divide an application into independent services.",
                List.of(
                        "Services can be deployed independently.",
                        "Each service owns a specific business capability."
                ),
                "MOCK",
                "mock-v1",
                null,
                null
        );

        assertDoesNotThrow(() -> validator.validate(response));
    }

    @Test
    void shouldRejectEmptySummary() {

        AiResponse response = new AiResponse(
                "",
                List.of("First takeaway"),
                "MOCK",
                "mock-v1",
                null,
                null
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(response)
        );
    }
}
