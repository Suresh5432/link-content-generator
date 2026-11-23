package com.linklens.summarizationservice.websiteanalysis;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebsiteAnalysisReportValidatorTest {

    @Test
    void requiresStructuredJsonWithAllReportSections() {
        ObjectMapper mapper = mock(ObjectMapper.class);
        JsonNode object = mock(JsonNode.class);
        JsonNode section = mock(JsonNode.class);
        when(mapper.readTree(anyString())).thenReturn(object);
        when(object.isObject()).thenReturn(true);
        when(object.get(anyString())).thenReturn(section);
        when(section.isNull()).thenReturn(false);
        when(section.isObject()).thenReturn(true);

        assertDoesNotThrow(() -> new WebsiteAnalysisReportValidator(mapper).validate("{}"));

        when(object.get("privacy")).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> new WebsiteAnalysisReportValidator(mapper).validate("{}"));
    }

    @Test
    void rejectsNonObjectOutput() {
        ObjectMapper mapper = mock(ObjectMapper.class);
        JsonNode array = mock(JsonNode.class);
        when(mapper.readTree(anyString())).thenReturn(array);
        when(array.isObject()).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> new WebsiteAnalysisReportValidator(mapper).validate("[]"));
    }
}
