package com.linklens.userservice.controller;

import com.linklens.userservice.dto.UpdateUserProfileRequest;
import com.linklens.userservice.dto.UserProfileResponse;
import com.linklens.userservice.entity.UserProfile;
import com.linklens.userservice.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getUserProfile(
            @AuthenticationPrincipal Jwt jwt){

        String userId=jwt.getSubject();

        return ResponseEntity.ok(userProfileService.getOrCreateProfile(userId));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserProfileResponse> updateUserProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateUserProfileRequest request){

        String userId=jwt.getSubject();

        return ResponseEntity.ok(userProfileService.updateProfile(userId,request));
    }

}
