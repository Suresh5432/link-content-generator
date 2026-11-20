package com.linklens.notificationservice.client;

import com.linklens.notificationservice.client.dto.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestClient userServiceRestCLient;

    public UserProfileResponse getUserProfile(String userID){
            return userServiceRestCLient.get()
                    .uri("/internal/v1/users/{userId}", userID)
                    .retrieve()
                    .body(UserProfileResponse.class);
    }
}
