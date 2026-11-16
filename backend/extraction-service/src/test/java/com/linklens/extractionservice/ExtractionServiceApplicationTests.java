package com.linklens.extractionservice;

import com.linklens.extractionservice.dto.ExtractedPage;
import com.linklens.extractionservice.service.ContentExtractor;
import com.linklens.extractionservice.service.SafeWebFetcher;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ExtractionServiceApplicationTests {

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
                        html.getBytes(StandardCharsets.UTF_8)
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

}
