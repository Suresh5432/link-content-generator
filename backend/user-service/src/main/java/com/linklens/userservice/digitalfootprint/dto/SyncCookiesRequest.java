package com.linklens.userservice.digitalfootprint.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record SyncCookiesRequest(
        @NotBlank String installationId,
        @NotNull @Valid List<CookieMetadata> cookies
) {}
