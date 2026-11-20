package com.linklens.userservice.service;

import com.linklens.userservice.dto.UpdateUserProfileRequest;
import com.linklens.userservice.dto.UserProfileResponse;
import com.linklens.userservice.entity.UserProfile;
import com.linklens.userservice.exception.UserProfileNotFoundException;
import com.linklens.userservice.repo.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository repository;

    @Transactional
    public UserProfileResponse getOrCreateProfile(
            String userId,
            String email){

        UserProfile profile =repository.findById(userId)
                .orElseGet(()-> new UserProfile(userId));

        if (email!=null &&!email.isBlank()
                && !email.equals(profile.getEmail())) {

            profile.setEmail(email);
        }

        return toResponse(repository.save(profile));
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


        return toResponse( repository.save(profile));
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String userId){

        return repository.findById(userId)
                .map(this::toResponse)
                .orElseThrow(()->
                        new UserProfileNotFoundException(userId));
    }

    private UserProfileResponse toResponse(UserProfile profile){
        return new UserProfileResponse(
                profile.getUserId(),
                profile.getDisplayName(),
                profile.getEmail(),
                profile.getBio(),
                profile.getAvatarUrl(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}
