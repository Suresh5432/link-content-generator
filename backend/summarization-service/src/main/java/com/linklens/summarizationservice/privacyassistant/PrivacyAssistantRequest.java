package com.linklens.summarizationservice.privacyassistant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PrivacyAssistantRequest(@NotBlank @Size(max = 2000) String question) {}
