package com.linklens.userservice.digitalfootprint.controller;

import com.linklens.userservice.digitalfootprint.dto.FootprintContextResponse;
import com.linklens.userservice.digitalfootprint.service.FootprintContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/internal/v1/users")
@RequiredArgsConstructor
public class InternalFootprintContextController {

    private final FootprintContextService contextService;

    @Value("${security.internal-client-ids:extraction-service,summarization-service,notification-service,link-service}")
    private List<String> internalClientIds;

    @GetMapping("/{userId}/footprint-context")
    public FootprintContextResponse getContext(@PathVariable String userId,
                                               @AuthenticationPrincipal Jwt jwt) {
        String clientId = jwt == null ? null : jwt.getClaimAsString("azp");
        if (clientId == null || !internalClientIds.contains(clientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Service client is not authorized");
        }
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }
        return contextService.buildContext(userId);
    }
}
