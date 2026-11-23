package com.linklens.extractionservice;

import com.linklens.extractionservice.dto.ExtractedPage;
import com.linklens.extractionservice.service.ContentExtractor;
import com.linklens.extractionservice.service.SafeWebFetcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.rabbitmq.username=guest",
        "spring.rabbitmq.password=guest",
        "spring.security.oauth2.client.registration.extraction-service.client-id=test",
        "spring.security.oauth2.client.registration.extraction-service.client-secret=test"
})
@Testcontainers(disabledWithoutDocker = true)
class ExtractionServiceApplicationTests {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("extraction_test_db")
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

    private final ContentExtractor extractor = new ContentExtractor();

    @Test
    void shouldExtractArticleMetadataAndText() {
        String html= """
                <!DOCTYPE html>
                                <html lang="en">
                                <head>
                                    <title>Example Article</title>
                                    <meta name="description"
                                          content="This is an example article.">
                                    <meta property="og:type" content="article">
                                    <link rel="canonical"
                                          href="https://example.com/article">
                                </head>
                                <body>
                                    <nav>Navigation content</nav>
                                    <article>
                                        <h1>Understanding Microservices</h1>
                                        <p>
                                            Microservices are an architectural style
                                            where applications are built as independent
                                            services.
                                        </p>
                                        <p>
                                            Each service can be developed and deployed
                                            independently.
                                        </p>
                                    </article>
                                    <footer>Footer content</footer>
                                </body>
                                </html>
                """;
        SafeWebFetcher.FetchedPage fetchedPage=
                new SafeWebFetcher.FetchedPage(
                        "https://example.com/article",
                        "text/html",
                        html.getBytes(StandardCharsets.UTF_8),
                        java.util.Map.of()
                );
        ExtractedPage result=extractor.extract(fetchedPage);

        assertEquals("Example Article", result.getTitle());

        assertEquals("This is an example article.", result.getDescription());

        assertEquals("ARTICLE", result.getContentType());
        assertEquals("en", result.getLanguage());

        assertTrue(
                result.getExtractedText().contains("Microservices")
        );

        assertFalse(
                result.getExtractedText().contains("Navigation content")
        );
    }

    @Test
    void websiteAnalysisMigrationsCreateExpectedSchema() {
        for (String table : new String[]{"website_analyses", "website_pages", "website_findings"}) {
            Integer count = jdbc.queryForObject(
                    "select count(*) from information_schema.tables where table_schema = 'public' and table_name = ?",
                    Integer.class, table);
            assertEquals(1, count, "missing table " + table);
        }
    }
}
