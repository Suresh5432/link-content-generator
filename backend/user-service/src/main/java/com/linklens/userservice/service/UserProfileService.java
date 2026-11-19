package com.linklens.userservice.service;

import com.linklens.userservice.dto.UpdateUserProfileRequest;
import com.linklens.userservice.dto.UserProfileResponse;
import com.linklens.userservice.entity.UserProfile;
import com.linklens.userservice.repo.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository repository;

    @Transactional
    public UserProfileResponse getOrCreateProfile(String userId){

        UserProfile profile =repository.findById(userId)
                .orElseGet(()->repository.save(new UserProfile(userId)));

        return toResponse(profile);
    }

    @Transactional
    public UserProfileResponse updateProfile(String userId,
                                             UpdateUserProfileRequest request){
        UserProfile profile=repository.findById(userId)
                .orElseGet(()->new UserProfile(userId));

        profile.updateProfile(
                request.displayName(),
                request.bio(),
                request.avatarUrl());

        UserProfile savedProfile= repository.save(profile);

        return toResponse(savedProfile);
    }

    private UserProfileResponse toResponse(UserProfile profile){
        return new UserProfileResponse(
                profile.getUserId(),
                profile.getDisplayName(),
                profile.getBio(),
                profile.getAvatarUrl(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}
