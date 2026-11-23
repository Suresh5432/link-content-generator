package com.linklens.userservice.digitalfootprint.controller;

import com.linklens.userservice.digitalfootprint.dto.CreateUserWebsiteAccountRequest;
import com.linklens.userservice.digitalfootprint.dto.UpdateUserWebsiteAccountRequest;
import com.linklens.userservice.digitalfootprint.entity.UserWebsiteAccount;
import com.linklens.userservice.digitalfootprint.service.UserWebsiteAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/digital-footprint/accounts")
public class UserWebsiteAccountController {

    private final UserWebsiteAccountService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserWebsiteAccount create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateUserWebsiteAccountRequest request) {
        return service.create(jwt.getSubject(), request);
    }

    @GetMapping
    public List<UserWebsiteAccount> list(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) UUID websiteId) {
        return websiteId == null ? service.listAll(jwt.getSubject()) : service.listForWebsite(jwt.getSubject(), websiteId);
    }

    @PatchMapping("/{id}")
    public UserWebsiteAccount update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @RequestBody UpdateUserWebsiteAccountRequest request) {
        return service.update(jwt.getSubject(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        service.delete(jwt.getSubject(), id);
    }
}
