package com.linklens.summarizationservice;

import com.linklens.summarizationservice.service.ai.AiInputValidator;
import com.linklens.summarizationservice.service.ai.AiRequest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AiInputValidatorTest {
    private final AiInputValidator validator = new AiInputValidator(5);
    @Test void acceptsContentWithinLimit() { assertNotNull(validator.validate(new AiRequest("title", "12345", "en"))); }
    @Test void rejectsEmptyOrOversizedContentAndMetadata() {
        assertThrows(IllegalArgumentException.class, () -> validator.validate(new AiRequest(null, " ", null)));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(new AiRequest(null, "123456", null)));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(new AiRequest("x".repeat(1001), "ok", null)));
    }
}
