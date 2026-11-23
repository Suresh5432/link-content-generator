package com.linklens.userservice;

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
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class UserServiceApplicationTests {

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
    private JdbcTemplate jdbc;

    @Test
    void contextLoads() {
    }

    @Test
    void digitalFootprintMigrationsCreateExpectedSchema() {
        for (String table : new String[]{
                "tracked_websites", "personal_data_records", "privacy_requests",
                "user_website_accounts", "cookie_inventory", "privacy_request_evidence",
                "privacy_request_history", "website_privacy_contacts", "user_privacy_preferences",
                "browser_integrations", "browser_sync_history"}) {
            Integer count = jdbc.queryForObject(
                    "select count(*) from information_schema.tables where table_schema = 'public' and table_name = ?",
                    Integer.class, table);
            assertEquals(1, count, "missing table " + table);
        }

        for (String column : new String[]{"last_inspection_at", "data_categories", "tracking_technologies", "deletion_request_status", "from_analysis_id", "first_recorded_at"}) {
            Integer count = jdbc.queryForObject(
                    "select count(*) from information_schema.columns where table_name = 'tracked_websites' and column_name = ?",
                    Integer.class, column);
            assertEquals(1, count, "missing tracked_websites column " + column);
        }

        for (String column : new String[]{"confirmed_by_user", "observed_on_website", "declared_in_policy", "inferred_by_ai", "date_shared", "evidence_source", "last_verified_at"}) {
            Integer count = jdbc.queryForObject(
                    "select count(*) from information_schema.columns where table_name = 'personal_data_records' and column_name = ?",
                    Integer.class, column);
            assertEquals(1, count, "missing personal_data_records column " + column);
        }

        for (String column : new String[]{"approved_at", "submitted_at", "submission_method", "website_claimed_deletion", "user_verified", "reminder_at"}) {
            Integer count = jdbc.queryForObject(
                    "select count(*) from information_schema.columns where table_name = 'privacy_requests' and column_name = ?",
                    Integer.class, column);
            assertEquals(1, count, "missing privacy_requests column " + column);
        }
        assertTrue(true);
    }
}
