package com.linklens.userservice.controller;

import com.linklens.userservice.dto.UserProfileResponse;
import com.linklens.userservice.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserProfileService userProfileService;

    @GetMapping("/userId")
    public ResponseEntity<UserProfileResponse> getUserProfile(
            @RequestParam String userId) {

        return ResponseEntity.ok(userProfileService.getProfile(userId));
    }
}
