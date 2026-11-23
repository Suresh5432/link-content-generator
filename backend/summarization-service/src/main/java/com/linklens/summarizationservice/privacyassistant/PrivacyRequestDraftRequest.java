package com.linklens.summarizationservice.privacyassistant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PrivacyRequestDraftRequest(
        @NotBlank @Size(max = 40) String requestType,
        @NotBlank @Size(max = 300) String websiteName,
        @Size(max = 1000) String dataCategories
) {}
