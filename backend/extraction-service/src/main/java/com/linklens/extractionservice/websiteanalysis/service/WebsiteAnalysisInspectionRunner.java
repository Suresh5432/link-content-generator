package com.linklens.extractionservice.websiteanalysis.service;

import com.linkcontext.contracts.EventRoutingKey;
import com.linkcontext.contracts.events.WebsiteAnalysisCompletedEvent;
import com.linklens.extractionservice.service.SafeWebFetcher;
import com.linklens.extractionservice.service.ContentExtractor;
import com.linklens.extractionservice.service.UrlSafetyValidator;
import com.linklens.extractionservice.websiteanalysis.WebsiteAnalysisStatus;
import com.linklens.extractionservice.websiteanalysis.config.WebsiteAnalysisProperties;
import com.linklens.extractionservice.websiteanalysis.entity.WebsiteAnalysis;
import com.linklens.extractionservice.websiteanalysis.entity.WebsiteFinding;
import com.linklens.extractionservice.websiteanalysis.entity.WebsitePage;
import com.linklens.extractionservice.websiteanalysis.repo.WebsiteAnalysisRepository;
import com.linklens.extractionservice.websiteanalysis.repo.WebsiteFindingRepository;
import com.linklens.extractionservice.entity.ExtractionOutboxEvent;
import com.linklens.extractionservice.repo.ExtractionOutboxRepository;
import com.linklens.extractionservice.websiteanalysis.repo.WebsitePageRepository;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.scheduling.annotation.Async;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import com.linklens.extractionservice.websiteanalysis.event.WebsiteAnalysisCreatedEvent;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Component
@Slf4j
public class WebsiteAnalysisInspectionRunner {

    private final Object rateLimitLock = new Object();
    private long lastRequestStartMillis;
    private final WebsiteAnalysisRepository analysisRepository;
    private final WebsitePageRepository pageRepository;
    private final WebsiteFindingRepository findingRepository;
    private final SafeWebFetcher fetcher;
    private final WebsiteAnalysisProperties properties;
    private final ExtractionOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final ContentExtractor contentExtractor;
    private final UrlSafetyValidator urlSafetyValidator;

    public WebsiteAnalysisInspectionRunner(
            WebsiteAnalysisRepository analysisRepository,
            WebsitePageRepository pageRepository,
            WebsiteFindingRepository findingRepository,
            @Qualifier("websiteAnalysisFetcher") SafeWebFetcher fetcher,
            WebsiteAnalysisProperties properties,
            ExtractionOutboxRepository outboxRepository,
            ObjectMapper objectMapper,
            ContentExtractor contentExtractor,
            UrlSafetyValidator urlSafetyValidator) {
        this.analysisRepository = analysisRepository;
        this.pageRepository = pageRepository;
        this.findingRepository = findingRepository;
        this.fetcher = fetcher;
        this.properties = properties;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.contentExtractor = contentExtractor;
        this.urlSafetyValidator = urlSafetyValidator;
    }

    @Async("websiteAnalysisExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onCreated(WebsiteAnalysisCreatedEvent event) {
        java.util.UUID analysisId = event.analysisId();
        WebsiteAnalysis analysis = analysisRepository.findById(analysisId).orElse(null);
        if (analysis == null) return;
        int successCount = 0;
        int failureCount = 0;
        Set<String> visited = new HashSet<>();
        Set<String> queued = new HashSet<>();
        Set<String> canonicalVisited = new HashSet<>();
        Instant deadline = Instant.now().plusSeconds(Math.max(1, properties.getOverallTimeoutSeconds()));
        try {
            analysis.setStatus(WebsiteAnalysisStatus.CRAWLING);
            analysisRepository.save(analysis);

            Deque<String[]> queue = new ArrayDeque<>();
            queue.add(new String[]{analysis.getNormalizedUrl(), "0"});
            queued.add(normalizeUrl(analysis.getNormalizedUrl()));
            RobotsPolicy robotsPolicy = properties.isRobotsEnabled()
                    ? fetchRobots(analysis.getNormalizedUrl(), analysisId)
                    : RobotsPolicy.allowAll();
            java.net.URI origin = URI.create(analysis.getNormalizedUrl());
            while (!queue.isEmpty() && visited.size() < properties.getMaxPages() && Instant.now().isBefore(deadline)) {
                String[] item = queue.poll();
                String url = item[0];
                int depth = Integer.parseInt(item[1]);
                String normalized = normalizeUrl(url);
                if (normalized == null) { failureCount++; continue; }
                queued.remove(normalized);
                if (visited.contains(normalized)) continue;
                try {
                    urlSafetyValidator.validate(normalized);
                } catch (RuntimeException e) {
                    recordFinding(analysisId, null, "PAGE_BLOCKED", "Unsafe URL", "Discovered URL was rejected by URL safety validation", normalized, "HIGH");
                    failureCount++;
                    continue;
                }
                visited.add(normalized);

                if (!robotsPolicy.allows(normalized)) {
                    recordFinding(analysisId, null, "PAGE_BLOCKED", "robots.txt", "Blocked by robots.txt", normalized, "HIGH");
                    failureCount++;
                    continue;
                }

                try {
                    waitForCrawlRateLimit();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    failureCount++;
                    break;
                }
                if (!Instant.now().isBefore(deadline)) {
                    visited.remove(normalized);
                    queued.add(normalized);
                    queue.addFirst(item);
                    break;
                }
                SafeWebFetcher.FetchedPage fetched;
                try {
                    fetched = fetcher.fetch(normalized);
                } catch (Exception e) {
                    log.warn("Failed to fetch {} during analysis {}", normalized, analysisId, e);
                    recordFinding(analysisId, null, "PAGE_ERROR", normalized, "Fetch failed: " + e.getMessage(), normalized, "HIGH");
                    failureCount++;
                    continue;
                }

                Document doc;
                try {
                    doc = Jsoup.parse(new String(fetched.body(), StandardCharsets.UTF_8), fetched.finalUrl());
                } catch (Exception e) {
                    recordFinding(analysisId, null, "PAGE_ERROR", fetched.finalUrl(), "Parse failed", fetched.finalUrl(), "HIGH");
                    failureCount++;
                    continue;
                }

                String canonical = extractCanonical(doc);
                if (canonical != null) {
                    String cn = normalizeUrl(canonical);
                    if (cn != null && sameOrigin(cn, origin) && canonicalVisited.contains(cn)) {
                        // duplicate canonical, skip
                        continue;
                    }
                    if (cn != null && sameOrigin(cn, origin)) canonicalVisited.add(cn);
                }

                // Prioritize important pages
                for (Element a : doc.select("a[href]")) {
                    String text = (a.text() + " " + a.attr("href")).toLowerCase(Locale.ROOT);
                    if (text.contains("privacy") || text.contains("terms") || text.contains("cookie") || text.contains("about") || text.contains("contact")) {
                        String resolved = resolve(a.attr("href"), fetched.finalUrl());
                        if (resolved != null && sameOrigin(resolved, origin)) {
                            String n = normalizeUrl(resolved);
                            if (n != null && !visited.contains(n)
                                    && visited.size() + queue.size() < properties.getMaxPages()
                                    && queued.add(n)) {
                                queue.addFirst(new String[]{resolved, String.valueOf(Math.min(depth + 1, properties.getMaxDepth()))});
                            }
                        }
                    }
                }

                WebsitePage page = WebsitePage.builder()
                        .analysisId(analysisId)
                        .url(fetched.finalUrl())
                        .canonicalUrl(canonical)
                        .title(doc.title())
                        .statusCode(fetched.statusCode())
                        .contentType(fetched.contentType())
                        .depth(depth)
                        .safetyNotes(buildSafety(doc, fetched))
                        .contentExcerpt(extractContentExcerpt(fetched, doc))
                        .fetchedAt(Instant.now())
                        .build();
                page = pageRepository.save(page);
                successCount++;

                detectFindings(analysisId, page.getId(), doc, fetched);

                if (depth < properties.getMaxDepth()) {
                    for (Element a : doc.select("a[href]")) {
                        String href = a.attr("href");
                        String resolved = resolve(href, fetched.finalUrl());
                        if (resolved == null) continue;
                        if (!sameOrigin(resolved, origin)) continue;
                        String n = normalizeUrl(resolved);
                        if (n == null) continue;
                        boolean notVisited = !visited.contains(n);
                        boolean underLimit = visited.size() + queue.size() < properties.getMaxPages();
                        if (notVisited && underLimit && queued.add(n)) {
                            queue.add(new String[]{resolved, String.valueOf(depth + 1)});
                        }
                    }
                }
            }

            boolean timedOut = !queue.isEmpty() && !Instant.now().isBefore(deadline);
            if (timedOut) {
                failureCount += queue.size();
                recordFinding(analysisId, null, "INSPECTION_LIMIT", "Overall timeout",
                        "Inspection ended at its configured overall timeout; queued pages were not inspected",
                        "Configured timeout: " + properties.getOverallTimeoutSeconds() + " seconds", "HIGH");
            }
            boolean pageLimitReached = !queue.isEmpty() && visited.size() >= properties.getMaxPages();
            if (pageLimitReached) {
                failureCount += queue.size();
                recordFinding(analysisId, null, "INSPECTION_LIMIT", "Page limit",
                        "Inspection stopped at its configured page limit; queued pages were not inspected",
                        "Configured limit: " + properties.getMaxPages() + " pages", "HIGH");
            }
            analysis.setStatus(successCount > 0 && failureCount == 0 ? WebsiteAnalysisStatus.COMPLETED
                    : (successCount > 0 ? WebsiteAnalysisStatus.PARTIALLY_COMPLETED : WebsiteAnalysisStatus.FAILED));
            analysis.setCompletedAt(Instant.now());
            analysis.setRiskLevel(assessRisk(analysisId));
            analysis.setConfidence(successCount == 0 ? 20 : Math.min(95, 35 + (int) (60.0 * successCount / Math.max(1, successCount + failureCount))));
            List<WebsitePage> pages = pageRepository.findAllByAnalysisId(analysisId);
            if (!pages.isEmpty()) {
                WebsitePage homepage = pages.stream().min(Comparator.comparingInt(WebsitePage::getDepth)).orElseThrow();
                analysis.setTitle(homepage.getTitle());
                analysis.setCategory(classifyCategory(homepage.getTitle() + " " + homepage.getContentExcerpt()));
            } else {
                analysis.setCategory("Other");
            }
            analysis.setReport(buildInspectionSummary(successCount, failureCount, timedOut || pageLimitReached, analysis.getRiskLevel()));
            try {
                WebsiteAnalysisCompletedEvent completedEvent = new WebsiteAnalysisCompletedEvent(
                        java.util.UUID.randomUUID(),
                        analysis.getId(),
                        analysis.getUserId(),
                        java.time.Instant.now(),
                        1);
                outboxRepository.save(ExtractionOutboxEvent.builder()
                        .id(completedEvent.eventId())
                        .aggregateId(analysis.getId())
                        .eventType(EventRoutingKey.WEBSITE_ANALYSIS_COMPLETED)
                        .payload(objectMapper.writeValueAsString(completedEvent))
                        .build());
            } catch (Exception e) {
                log.warn("Failed to queue website analysis completed event", e);
            }
            analysisRepository.save(analysis);
        } catch (Exception e) {
            log.error("Website analysis failed {}", analysisId, e);
            analysis.setStatus(WebsiteAnalysisStatus.FAILED);
            analysis.setErrorMessage(e.getMessage());
            analysis.setCompletedAt(Instant.now());
            analysisRepository.save(analysis);
        }
    }

    private RobotsPolicy fetchRobots(String normalizedUrl, UUID analysisId) {
        try {
            URI uri = new URI(normalizedUrl);
            String robotsUrl = uri.getScheme() + "://" + uri.getRawAuthority() + "/robots.txt";
            waitForCrawlRateLimit();
            SafeWebFetcher.FetchedPage fetched = fetcher.fetchRaw(robotsUrl);
            if (fetched.statusCode() == 404) {
                return RobotsPolicy.allowAll();
            }
            if (fetched.statusCode() != 200) {
                throw new IllegalStateException("robots.txt returned HTTP " + fetched.statusCode());
            }
            String text = new String(fetched.body(), StandardCharsets.UTF_8);
            return RobotsPolicy.parse(text, "LinkContextBot");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while checking robots.txt for analysis {}", analysisId, e);
            recordFinding(analysisId, null, "ROBOTS_POLICY_UNAVAILABLE", "robots.txt",
                    "Crawling was blocked because robots.txt policy could not be verified",
                    "robots.txt fetch interrupted; no website pages were requested", "LOW");
            return RobotsPolicy.denyAll();
        } catch (Exception e) {
            log.warn("Could not determine robots.txt policy for analysis {}", analysisId, e);
            recordFinding(analysisId, null, "ROBOTS_POLICY_UNAVAILABLE", "robots.txt",
                    "Crawling was blocked because robots.txt policy could not be verified",
                    "robots.txt fetch failed; no website pages were requested", "LOW");
            return RobotsPolicy.denyAll();
        }
    }

    private void waitForCrawlRateLimit() throws InterruptedException {
        synchronized (rateLimitLock) {
            long delay = lastRequestStartMillis + Math.max(0, properties.getRateLimitMillis()) - System.currentTimeMillis();
            if (delay > 0) {
                Thread.sleep(delay);
            }
            lastRequestStartMillis = System.currentTimeMillis();
        }
    }

    private void recordFinding(java.util.UUID analysisId, java.util.UUID pageId, String type, String name, String description, String evidence, String confidence) {
        WebsiteFinding f = WebsiteFinding.builder()
                .analysisId(analysisId)
                .pageId(pageId)
                .type(limit(type, 60))
                .name(limit(name, 255))
                .description(limit(description, 2000))
                .evidence(limit(evidence, 4000))
                .confidence(limit(confidence, 20))
                .build();
        findingRepository.save(f);
    }

    private void detectFindings(java.util.UUID analysisId, java.util.UUID pageId, Document doc, SafeWebFetcher.FetchedPage fetched) {
        int detectedInputs = 0;
        boolean inputLimitReached = false;
        for (Element form : doc.select("form")) {
            for (Element input : form.select("input,textarea,select")) {
                if (detectedInputs >= 100) {
                    inputLimitReached = true;
                    break;
                }
                detectedInputs++;
                String name = input.attr("name");
                if (name.isBlank()) name = input.attr("id");
                if (name.isBlank()) name = input.attr("placeholder");
                if (name.isBlank()) name = input.attr("type");
                String type = input.attr("type");
                String label = findLabel(doc, input);
                String category = classifyInput(name, type, label);
                if (category != null) {
                    recordFinding(analysisId, pageId, "INPUT_FIELD", name, "Detected input requesting " + category, "type=" + type + ", label=" + label, "MEDIUM");
                }
            }
            if (inputLimitReached) break;
        }
        if (inputLimitReached) {
            recordFinding(analysisId, pageId, "INSPECTION_LIMIT", "Input scan limit",
                    "Only the first 100 form inputs on this page were inspected", fetched.finalUrl(), "HIGH");
        }

        String[] trackerKeywords = {"google-analytics", "googletagmanager", "facebook.net", "doubleclick", "hotjar", "segment.com", "mixpanel"};
        Set<String> detectedTrackers = new HashSet<>();
        for (Element script : doc.select("script[src]")) {
            String src = script.attr("src").toLowerCase(Locale.ROOT);
            for (String kw : trackerKeywords) {
                if (src.contains(kw) && detectedTrackers.add(kw)) {
                    recordFinding(analysisId, pageId, "TRACKER", kw, "Detected tracking/analytics script (script found, not proof of data transmission)", src, "HIGH");
                    break;
                }
            }
        }

        int iframeCount = 0;
        for (Element iframe : doc.select("iframe[src]")) {
            if (iframeCount++ == 50) break;
            recordFinding(analysisId, pageId, "THIRD_PARTY", "iframe", "Embedded external iframe", iframe.attr("src"), "MEDIUM");
        }

        // Cookies (metadata only, never values)
        for (Map.Entry<String,String> h : (fetched.headers() == null ? Map.<String,String>of() : fetched.headers()).entrySet()) {
            if (h.getKey().equalsIgnoreCase("Set-Cookie")) {
                String cookie = h.getValue();
                String name = cookie.split(";", 2)[0].split("=", 2)[0].trim();
                boolean secure = cookie.toLowerCase(Locale.ROOT).contains("secure");
                boolean httpOnly = cookie.toLowerCase(Locale.ROOT).contains("httponly");
                String sameSite = cookie.toLowerCase(Locale.ROOT).contains("samesite") ? "present" : "missing";
                recordFinding(analysisId, pageId, "COOKIE", name, "Observed Set-Cookie (value not stored)", "Secure=" + secure + ", HttpOnly=" + httpOnly + ", SameSite=" + sameSite, "HIGH");
            }
        }

        // Browser storage references (static evidence only)
        String html = doc.html().toLowerCase(Locale.ROOT);
        if (html.contains("localstorage")) recordFinding(analysisId, pageId, "STORAGE", "localStorage", "Page references localStorage (observed in source, not runtime storage)", "localStorage", "MEDIUM");
        if (html.contains("sessionstorage")) recordFinding(analysisId, pageId, "STORAGE", "sessionStorage", "Page references sessionStorage (observed in source, not runtime storage)", "sessionStorage", "MEDIUM");

        // Open Graph metadata
        for (Element meta : doc.select("meta[property^=og:]")) {
            recordFinding(analysisId, pageId, "METADATA", meta.attr("property"), "Open Graph metadata", meta.attr("content"), "HIGH");
        }
        // JSON-LD
        for (Element script : doc.select("script[type=application/ld+json]")) {
            recordFinding(analysisId, pageId, "METADATA", "json-ld", "Structured data (JSON-LD)", excerpt(script.data()), "MEDIUM");
        }
        // Favicon
        Element favicon = doc.selectFirst("link[rel~=(?i)icon]");
        if (favicon != null) {
            recordFinding(analysisId, pageId, "METADATA", "favicon", "Favicon", favicon.attr("href"), "HIGH");
        }

        Map<String, String> headers = fetched.headers() == null ? Map.of() : fetched.headers();
        checkHeader(analysisId, pageId, headers, "Strict-Transport-Security", "SECURITY", "HSTS header");
        checkHeader(analysisId, pageId, headers, "Content-Security-Policy", "SECURITY", "CSP header");
        checkHeader(analysisId, pageId, headers, "X-Frame-Options", "SECURITY", "X-Frame-Options header");
        checkHeader(analysisId, pageId, headers, "X-Content-Type-Options", "SECURITY", "X-Content-Type-Options header");
        checkHeader(analysisId, pageId, headers, "Referrer-Policy", "SECURITY", "Referrer-Policy header");

        detectPrivacyDiscovery(analysisId, pageId, doc, fetched);
    }

    /**
     * Discovers publicly linked privacy resources. Findings record the source URL as
     * evidence; nothing here is treated as a legal guarantee or a verified contact.
     */
    private void detectPrivacyDiscovery(java.util.UUID analysisId, java.util.UUID pageId, Document doc, SafeWebFetcher.FetchedPage fetched) {
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (Element a : doc.select("a[href]")) {
            String href = a.attr("href");
            if (href == null || href.isBlank()) continue;
            if (href.toLowerCase(Locale.ROOT).startsWith("mailto:")) {
                String email = href.substring(7).split("\\?", 2)[0].trim();
                if (PrivacyDiscoveryClassifier.isPrivacyEmail(email) && seen.add("email:" + email)) {
                    recordFinding(analysisId, pageId, "PRIVACY_CONTACT_EMAIL", email,
                            "Publicly linked privacy contact email (verify before use)", resolvedOrRaw(href, fetched.finalUrl()), "MEDIUM");
                }
                continue;
            }
            String type = PrivacyDiscoveryClassifier.classifyLink(a.text(), href);
            if (type == null) continue;
            String resolved = resolve(href, fetched.finalUrl());
            if (resolved == null) continue;
            if (!seen.add(type + ":" + resolved)) continue;
            recordFinding(analysisId, pageId, type, a.text().isBlank() ? type.replace('_', ' ') : a.text(),
                    "Publicly linked " + type.replace('_', ' ').toLowerCase(Locale.ROOT) + " (verify before use)", resolved, "MEDIUM");
        }

        for (String email : PrivacyDiscoveryClassifier.privacyEmails(doc.text())) {
            if (seen.add("text-email:" + email)) {
                recordFinding(analysisId, pageId, "PRIVACY_CONTACT_EMAIL", email,
                        "Email address found in page text mentioning privacy (verify before use)", fetched.finalUrl(), "LOW");
            }
        }
    }

    private String resolvedOrRaw(String href, String base) {
        String resolved = resolve(href, base);
        return resolved == null ? href : resolved;
    }

    private void checkHeader(java.util.UUID analysisId, java.util.UUID pageId, Map<String, String> headers, String header, String type, String desc) {
        boolean present = headers.keySet().stream().anyMatch(h -> h.equalsIgnoreCase(header));
        String value = present ? headers.entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase(header)).findFirst().map(Map.Entry::getValue).orElse("") : "not present";
        recordFinding(analysisId, pageId, type, header, desc + (present ? " present" : " missing"), value, "HIGH");
    }

    private String classifyInput(String name, String type, String label) {
        String t = (name + " " + label + " " + type).toLowerCase(Locale.ROOT);
        if (t.contains("password")) return "password";
        if (t.contains("email")) return "email";
        if (t.contains("phone") || t.contains("tel")) return "phone";
        if (t.contains("card") || t.contains("cvv") || t.contains("expiry")) return "financial";
        if (t.contains("passport") || t.contains("ssn") || t.contains("aadhaar") || t.contains("pan")) return "government id";
        if (t.contains("health") || t.contains("medical")) return "health";
        if (type.equalsIgnoreCase("password")) return "password";
        if (type.equalsIgnoreCase("email")) return "email";
        if (type.equalsIgnoreCase("tel")) return "phone";
        return null;
    }

    private String findLabel(Document doc, Element input) {
        String id = input.attr("id");
        if (!id.isBlank()) {
            Element label = doc.select("label[for]").stream()
                    .filter(element -> element.attr("for").equals(id))
                    .findFirst()
                    .orElse(null);
            if (label != null) return label.text();
        }
        Element parentLabel = input.closest("label");
        if (parentLabel != null) return parentLabel.text();
        return input.attr("placeholder");
    }

    private String buildSafety(Document doc, SafeWebFetcher.FetchedPage fetched) {
        List<String> notes = new ArrayList<>();
        try {
            URI uri = new URI(fetched.finalUrl());
            notes.add("https".equalsIgnoreCase(uri.getScheme()) ? "HTTPS used." : "Not HTTPS.");
        } catch (Exception ignored) {}
        return String.join(" ", notes);
    }

    private String extractCanonical(Document doc) {
        Element c = doc.selectFirst("link[rel=canonical][href]");
        return c == null ? null : c.attr("href");
    }

    private String normalizeUrl(String raw) {
        try {
            URI uri = new URI(raw.trim()).normalize();
            String scheme = uri.getScheme() == null ? "https" : uri.getScheme();
            String host = uri.getHost();
            if (host == null) return null;
            String path = uri.getRawPath() == null || uri.getRawPath().isBlank() ? "/" : uri.getRawPath();
            String port = uri.getPort() < 0 ? "" : ":" + uri.getPort();
            return scheme.toLowerCase(Locale.ROOT) + "://" + host.toLowerCase(Locale.ROOT) + port + path;
        } catch (java.net.URISyntaxException e) {
            return null;
        }
    }

    private String domainOf(String normalized) {
        try { return new URI(normalized).getHost(); } catch (Exception e) { return null; }
    }

    private String resolve(String href, String base) {
        try {
            URI r = new URI(base).resolve(href);
            if (r.getScheme() == null || r.getHost() == null) return null;
            if (!"http".equalsIgnoreCase(r.getScheme()) && !"https".equalsIgnoreCase(r.getScheme())) return null;
            return r.toString();
        } catch (Exception e) { return null; }
    }

    private boolean sameOrigin(String url, URI origin) {
        try {
            URI candidate = new URI(url);
            int originPort = effectivePort(origin);
            return candidate.getScheme() != null
                    && candidate.getScheme().equalsIgnoreCase(origin.getScheme())
                    && candidate.getHost() != null
                    && candidate.getHost().equalsIgnoreCase(origin.getHost())
                    && effectivePort(candidate) == originPort;
        } catch (java.net.URISyntaxException e) {
            return false;
        }
    }

    private int effectivePort(URI uri) {
        if (uri.getPort() >= 0) return uri.getPort();
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    private String extractContentExcerpt(SafeWebFetcher.FetchedPage fetched, Document doc) {
        try {
            return excerpt(contentExtractor.extract(fetched).getExtractedText());
        } catch (RuntimeException e) {
            log.debug("Main-content extraction unavailable for {}", fetched.finalUrl(), e);
            return excerpt(doc.text());
        }
    }

    private String excerpt(String text) {
        if (text == null) return null;
        String t = text.replaceAll("\\s+", " ").trim();
        return t.length() > 500 ? t.substring(0, 500) : t;
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value;
        return value.substring(0, maxLength);
    }

    private String assessRisk(java.util.UUID analysisId) {
        var findings = findingRepository.findAllByAnalysisId(analysisId);
        if (pageRepository.findAllByAnalysisId(analysisId).isEmpty()) return "INSUFFICIENT_DATA";
        Set<String> sensitiveCategories = findings.stream()
                .filter(f -> "INPUT_FIELD".equals(f.getType()))
                .map(WebsiteFinding::getDescription)
                .filter(Objects::nonNull)
                .map(description -> description.replace("Detected input requesting ", ""))
                .filter(category -> !category.equals("email") && !category.equals("phone"))
                .collect(java.util.stream.Collectors.toSet());
        Set<String> highSensitivityCategories = findings.stream()
                .filter(f -> "INPUT_FIELD".equals(f.getType()))
                .map(WebsiteFinding::getDescription)
                .filter(Objects::nonNull)
                .map(description -> description.replace("Detected input requesting ", ""))
                .filter(category -> Set.of("financial", "government id", "health", "password").contains(category))
                .collect(java.util.stream.Collectors.toSet());
        long trackers = findings.stream().filter(f -> "TRACKER".equals(f.getType()))
                .map(WebsiteFinding::getName).filter(Objects::nonNull).distinct().count();
        long missingSecurityHeaders = findings.stream()
                .filter(f -> "SECURITY".equals(f.getType()) && f.getDescription() != null && f.getDescription().contains("missing"))
                .map(WebsiteFinding::getName).filter(Objects::nonNull).distinct().count();
        if (highSensitivityCategories.size() >= 2 || (!highSensitivityCategories.isEmpty() && trackers >= 4)) return "HIGH";
        if (!sensitiveCategories.isEmpty() || trackers >= 3 || missingSecurityHeaders >= 4) return "MODERATE";
        return "LOW";
    }

    private String classifyCategory(String evidence) {
        String text = evidence == null ? "" : evidence.toLowerCase(Locale.ROOT);
        if (containsAny(text, "shopping cart", "add to cart", "checkout", "product price")) return "E-commerce";
        if (containsAny(text, "bank account", "banking", "investment", "loan application", "financial services")) return "Banking and finance";
        if (containsAny(text, "medical care", "healthcare", "patient", "prescription", "medical services")) return "Healthcare";
        if (containsAny(text, "course", "student", "learning", "education", "university")) return "Education";
        if (containsAny(text, "social network", "user profile", "follow people", "community")) return "Social media";
        if (containsAny(text, "software platform", "saas", "subscription plans", "business dashboard")) return "SaaS";
        if (containsAny(text, "government services", "public services", "ministry", "municipality")) return "Government";
        if (containsAny(text, "news", "journalism", "latest headlines", "editorial")) return "News and media";
        if (containsAny(text, "streaming", "music", "movies", "entertainment")) return "Entertainment";
        if (containsAny(text, "hotel booking", "flight booking", "travel packages", "reservations")) return "Travel and booking";
        if (containsAny(text, "technology", "developer tools", "cloud computing", "api documentation")) return "Technology";
        return "Other";
    }

    private boolean containsAny(String text, String... phrases) {
        return Arrays.stream(phrases).anyMatch(text::contains);
    }

    private String buildInspectionSummary(int successfulPages, int failedPages, boolean timedOut, String risk) {
        String scope = successfulPages == 0
                ? "No pages were successfully inspected."
                : successfulPages + " page(s) were successfully inspected and " + failedPages + " page(s) were blocked or unavailable.";
        String limit = timedOut ? " The configured overall timeout was reached." : "";
        String rationale = switch (risk) {
            case "HIGH" -> " High risk reflects multiple observed sensitive-data requests or sensitive requests combined with extensive tracking.";
            case "MODERATE" -> " Moderate risk reflects observed sensitive-data requests, multiple recognized tracking scripts, or several missing security headers.";
            case "LOW" -> " Low risk means this limited inspection did not identify strong risk signals; it is not a safety guarantee.";
            default -> " Risk could not be assessed because no page content was available.";
        };
        return "Static inspection: " + scope + limit + rationale;
    }
}
