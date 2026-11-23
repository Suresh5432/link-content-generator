package com.linklens.summarizationservice;

import com.linklens.summarizationservice.service.ai.AiInputValidator;
import com.linklens.summarizationservice.service.ai.AiRequest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AiInputValidatorTest {
    private final AiInputValidator validator = new AiInputValidator(5);
    @Test void acceptsContentWithinLimit() { assertNotNull(validator.validate(new AiRequest("title", "12345", "en"))); }
    @Test void rejectsEmptyContentAndOversizedMetadataButTruncatesLargeContent() {
        assertThrows(IllegalArgumentException.class, () -> validator.validate(new AiRequest(null, " ", null)));
        AiRequest truncated = validator.validate(new AiRequest(null, "123456789", null));
        assertEquals(5, truncated.content().length());
        assertTrue(truncated.content().startsWith("1234"));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(new AiRequest("x".repeat(1001), "ok", null)));
    }

    @Test void truncationDoesNotSplitSurrogatePairs() {
        AiInputValidator unicodeValidator = new AiInputValidator(8);
        AiRequest truncated = unicodeValidator.validate(new AiRequest(null, "😀😀😀😀😀", null));
        assertTrue(truncated.content().length() <= 8);
        assertFalse(Character.isHighSurrogate(truncated.content().charAt(truncated.content().length() - 2)));
    }
}
