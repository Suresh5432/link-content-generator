package com.linklens.userservice.repo;

import com.linklens.userservice.entity.UserProfile;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class UserProfileRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("user_test_db")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {

        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private UserProfileRepository repository;

    @Test
    void shouldSaveAndRetrieveUserProfile() {

        UserProfile profile =
                new UserProfile("keycloak-test-user");

        profile.updateProfile(
                "Test User",
                "Integration testing",
                "https://example.com/avatar.png"
        );

        repository.save(profile);

        Optional<UserProfile> result =
                repository.findById("keycloak-test-user");

        assertTrue(result.isPresent());

        assertEquals(
                "Test User",
                result.get().getDisplayName()
        );

        assertEquals(
                "Integration testing",
                result.get().getBio()
        );
    }

    @Test
    void shouldDeleteUserProfile() {

        UserProfile profile =
                repository.save(
                        new UserProfile("delete-test-user")
                );

        repository.deleteById(profile.getUserId());

        assertFalse(
                repository.existsById("delete-test-user")
        );
    }
}