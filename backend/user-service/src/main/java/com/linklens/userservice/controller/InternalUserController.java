package com.linklens.userservice.controller;

import com.linklens.userservice.dto.UserProfileResponse;
import com.linklens.userservice.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal/v1/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserProfileService userProfileService;
    @Value("${security.internal-client-ids:extraction-service,summarization-service,notification-service,link-service}")
    private java.util.List<String> internalClientIds;

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> getUserProfile(
            @PathVariable String userId,
            @AuthenticationPrincipal Jwt jwt) {

        String clientId = jwt.getClaimAsString("azp");
        if (clientId == null || !internalClientIds.contains(clientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Service client is not authorized");
        }

        return ResponseEntity.ok(userProfileService.getProfile(userId));
    }
}
