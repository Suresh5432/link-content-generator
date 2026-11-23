package com.linklens.userservice.digitalfootprint.controller;

import com.linklens.userservice.digitalfootprint.dto.ConnectedEmailAccountResponse;
import com.linklens.userservice.digitalfootprint.dto.ConnectedEmailDiscoveryResponse;
import com.linklens.userservice.digitalfootprint.dto.ConnectedEmailScanResponse;
import com.linklens.userservice.digitalfootprint.dto.ConnectedEmailStatusResponse;
import com.linklens.userservice.digitalfootprint.exception.InvalidRequestException;
import com.linklens.userservice.digitalfootprint.service.ConnectedEmailOAuthService;
import com.linklens.userservice.digitalfootprint.service.GoogleEmailProviderException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/digital-footprint/connected-email")
public class ConnectedEmailDiscoveryController {
    private final ConnectedEmailOAuthService service;

    @Value("${digital-footprint.connected-email.frontend-url:http://localhost:5173/digital-footprint}")
    private String frontendUrl;

    @GetMapping("/status")
    public ConnectedEmailStatusResponse status() {
        return service.status();
    }

    @GetMapping("/accounts")
    public List<ConnectedEmailAccountResponse> accounts(@AuthenticationPrincipal Jwt jwt) {
        return service.accounts(jwt.getSubject());
    }

    @GetMapping("/discoveries")
    public List<ConnectedEmailDiscoveryResponse> discoveries(@AuthenticationPrincipal Jwt jwt) {
        return service.discoveries(jwt.getSubject());
    }

    @PostMapping("/oauth/google/start")
    public Map<String, String> start(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ConsentRequest request) {
        if (!request.consent()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Explicit consent is required.");
        return Map.of("authorizationUrl", service.beginAuthorization(jwt.getSubject(), true));
    }

    @GetMapping("/oauth/google/callback")
    public RedirectView callback(
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error
    ) {
        String result = "error";
        try {
            if (error != null) {
                service.rejectAuthorization(state);
            } else {
                service.completeAuthorization(state, code);
                result = "connected";
            }
        } catch (InvalidRequestException | GoogleEmailProviderException exception) {
            result = "error";
        }
        RedirectView redirect = new RedirectView(frontendUrl + "?emailDiscovery=" + result);
        redirect.setExposeModelAttributes(false);
        return redirect;
    }

    @PostMapping("/accounts/{id}/scan")
    public ConnectedEmailScanResponse scan(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.scan(jwt.getSubject(), id);
    }

    @PostMapping("/discoveries/{id}/confirm")
    public ConnectedEmailDiscoveryResponse confirm(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.confirm(jwt.getSubject(), id);
    }

    @PostMapping("/discoveries/{id}/dismiss")
    public ConnectedEmailDiscoveryResponse dismiss(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.dismiss(jwt.getSubject(), id);
    }

    @DeleteMapping("/discoveries/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDiscovery(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        service.deleteDiscovery(jwt.getSubject(), id);
    }

    @DeleteMapping("/accounts/{id}")
    public ConnectedEmailOAuthService.DisconnectResult disconnect(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id
    ) {
        return service.disconnect(jwt.getSubject(), id);
    }

    public record ConsentRequest(@NotNull Boolean consent) {}
}
