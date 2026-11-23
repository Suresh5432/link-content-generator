package com.linklens.notificationservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.rabbitmq.username=guest",
        "spring.rabbitmq.password=guest",
        "spring.mail.username=test@example.com",
        "spring.mail.password=test",
        "spring.security.oauth2.client.registration.user-service.client-secret=test",
        "notification.email.from=test@example.com"
})
@Testcontainers(disabledWithoutDocker = true)
class NotificationServiceApplicationTests {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("notification_test_db")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void contextLoads() {
    }

    @Test
    void privacyNotificationMigrationAddsRequestIdAndNullableLinkId() {
        Integer requestId = jdbc.queryForObject(
                "select count(*) from information_schema.columns where table_name = 'notifications' and column_name = 'request_id'",
                Integer.class);
        assertEquals(1, requestId);

        String linkNullable = jdbc.queryForObject(
                "select is_nullable from information_schema.columns where table_name = 'notifications' and column_name = 'link_id'",
                String.class);
        assertEquals("YES", linkNullable);
    }
}
