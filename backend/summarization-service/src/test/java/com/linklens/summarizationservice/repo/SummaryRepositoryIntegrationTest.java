package com.linklens.summarizationservice.repo;

import com.linklens.summarizationservice.entity.Summary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = "spring.test.database.replace=NONE")
@Testcontainers(disabledWithoutDocker = true)
class SummaryRepositoryIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("summary_test_db")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    SummaryRepository repository;

    @Test
    void listAndLookupQueriesNeverReturnAnotherUsersSummary() {
        Summary alice = summary("alice", Instant.parse("2026-01-02T00:00:00Z"));
        Summary bob = summary("bob", Instant.parse("2026-01-03T00:00:00Z"));
        repository.saveAll(List.of(alice, bob));

        var page = repository.findAllByUserId("alice", PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")));

        assertEquals(1, page.getTotalElements());
        assertEquals(alice.getId(), page.getContent().getFirst().getId());
        assertTrue(repository.findByIdAndUserId(alice.getId(), "bob").isEmpty());
        assertTrue(repository.findByLinkIdAndUserId(alice.getLinkId(), "bob").isEmpty());
        assertEquals(alice.getId(), repository.findByLinkIdAndUserId(alice.getLinkId(), "alice").orElseThrow().getId());
    }

    private Summary summary(String userId, Instant createdAt) {
        return Summary.builder()
                .id(UUID.randomUUID())
                .linkId(UUID.randomUUID())
                .contentId(UUID.randomUUID())
                .userId(userId)
                .title("Title for " + userId)
                .summary("Summary text")
                .keyTakeaways(List.of("Takeaway"))
                .readingTimeMinutes(1)
                .wordCount(10)
                .aiProvider("test-provider")
                .aiModel("test-model")
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
    }
}
