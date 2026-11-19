package com.linklens.userservice.service;

import com.linklens.userservice.dto.UpdateUserProfileRequest;
import com.linklens.userservice.dto.UserProfileResponse;
import com.linklens.userservice.entity.UserProfile;
import com.linklens.userservice.repo.UserProfileRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserProfileRepository repository;

    @InjectMocks
    private UserProfileService service;

    private final String userId = "keycloak-user-123";

    @Test
    void shouldCreateProfileWhenProfileDoesNotExist() {

        UserProfile profile = new UserProfile(userId);

        when(repository.findById(userId))
                .thenReturn(Optional.empty());

        when(repository.save(any(UserProfile.class)))
                .thenReturn(profile);

        UserProfileResponse response =
                service.getOrCreateProfile(userId);

        assertEquals(userId, response.userId());

        verify(repository).findById(userId);
        verify(repository).save(any(UserProfile.class));
    }

    @Test
    void shouldReturnExistingProfile() {

        UserProfile profile = new UserProfile(userId);

        when(repository.findById(userId))
                .thenReturn(Optional.of(profile));

        UserProfileResponse response =
                service.getOrCreateProfile(userId);

        assertEquals(userId, response.userId());

        verify(repository, never()).save(any());
    }

    @Test
    void shouldUpdateUserProfile() {

        UpdateUserProfileRequest request =
                new UpdateUserProfileRequest(
                        "Suresh",
                        "Java Developer",
                        "https://example.com/avatar.png"
                );

        when(repository.findById(userId))
                .thenReturn(Optional.empty());

        when(repository.save(any(UserProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponse response =
                service.updateProfile(userId, request);

        assertEquals("Suresh", response.displayName());
        assertEquals("Java Developer", response.bio());
        assertEquals(
                "https://example.com/avatar.png",
                response.avatarUrl()
        );

        verify(repository).save(any(UserProfile.class));
    }
}