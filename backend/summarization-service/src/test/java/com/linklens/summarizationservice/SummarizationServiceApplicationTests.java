package com.linklens.summarizationservice;

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
        "spring.security.oauth2.client.registration.extraction-service.client-id=test",
        "spring.security.oauth2.client.registration.extraction-service.client-secret=test"
})
@Testcontainers(disabledWithoutDocker = true)
class SummarizationServiceApplicationTests {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("summarization_test_db")
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
    void summaryMigrationsCreateExpectedSchema() {
        for (String table : new String[]{"summaries", "processed_events", "summary_outbox_events"}) {
            Integer count = jdbc.queryForObject(
                    "select count(*) from information_schema.tables where table_schema = 'public' and table_name = ?",
                    Integer.class, table);
            assertEquals(1, count, "missing table " + table);
        }

        Integer canonicalUrl = jdbc.queryForObject(
                "select count(*) from information_schema.columns where table_name = 'summaries' and column_name = 'canonical_url'",
                Integer.class);
        assertEquals(1, canonicalUrl);
    }
}
