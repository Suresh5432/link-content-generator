package com.linklens.extractionservice.service;

import com.linklens.extractionservice.exception.ExtractionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContentExtractorTest {

    private final ContentExtractor contentExtractor = new ContentExtractor();

    @Test
    void usesPageDescriptionWhenTheBodyHasNoReadableText() {
        String html = """
                <html><head>
                  <meta name="description" content="A useful description of this page.">
                </head><body><script>renderPage()</script></body></html>
                """;

        var page = contentExtractor.extract(fetchedPage(html));

        assertEquals("A useful description of this page.", page.getExtractedText());
    }

    @Test
    void marksPagesWithoutReadableTextOrDescriptionAsPermanentFailures() {
        String html = """
                <html><head><title></title></head>
                <body><script>renderPage()</script><noscript>Enable javascript</noscript></body></html>
                """;

        assertThrows(ExtractionException.class, () -> contentExtractor.extract(fetchedPage(html)));
    }

    @Test
    void extractsPublicProfileFieldsFromJsonLdWhenThePageHasNoReadableBody() {
        String html = """
                <html><head>
                  <script type="application/ld+json">
                    {
                      "@context": "https://schema.org",
                      "@type": "Person",
                      "name": "Ada Lovelace",
                      "jobTitle": "Software Engineer",
                      "description": "Builds tools for solving complex problems."
                    }
                  </script>
                </head><body><script>renderProfile()</script></body></html>
                """;

        var page = contentExtractor.extract(fetchedPage(html));

        assertEquals(
                "Ada Lovelace Software Engineer Builds tools for solving complex problems.",
                page.getExtractedText()
        );
        assertEquals(10, page.getWordCount());
    }

    private SafeWebFetcher.FetchedPage fetchedPage(String html) {
        return new SafeWebFetcher.FetchedPage(
                "https://example.com/article",
                "text/html; charset=UTF-8",
                html.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                java.util.Map.of());
    }
}
