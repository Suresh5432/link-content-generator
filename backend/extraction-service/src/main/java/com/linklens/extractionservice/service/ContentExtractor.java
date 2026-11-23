package com.linklens.extractionservice.service;

import com.linklens.extractionservice.dto.ExtractedPage;
import com.linklens.extractionservice.exception.ExtractionException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class ContentExtractor {

    private static final int MAX_TITLE_LENGTH=1000;
    private static final int MAX_DESCRIPTION_LENGTH=3000;
    private static final int MAX_TEXT_LENGTH=500_000;

    private static final Set<String> STRUCTURED_CONTENT_FIELDS = Set.of(
            "about",
            "articleBody",
            "description",
            "headline",
            "jobTitle",
            "keywords",
            "knowsAbout",
            "name",
            "text"
    );

    private static final Pattern WHITESPACE= Pattern.compile("\\s+");
    private static final Logger log = LoggerFactory.getLogger(ContentExtractor.class);
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private static final Set<String> REMOVE_SELECTORS=Set.of(
            "script",
            "style",
            "noscript",
            "svg",
            "canvas",
            "iframe",
            "nav",
            "footer",
            "header",
            "aside",
            "form",
            "button",
            "input",
            "select",
            "textarea"
    );

    public ExtractedPage extract(SafeWebFetcher.FetchedPage fetchedPage) {
        Document document = null;
        try {
            document = Jsoup.parse(
                    new String(fetchedPage.body(), detectCharset(fetchedPage)),
                    fetchedPage.finalUrl()
            );
        } catch (UnsupportedEncodingException e) {
            throw new ExtractionException("Unable to parse the webpage content using jsoup", e);
        }

        String title=extractTitle(document);
        String description=extractDescription(document);
        String canonicalUrl=extractCanonicalUrl(document);
        String language=extractLanguage(document);
        String structuredContent=extractStructuredContent(document);

        String extractedText=extractMainContent(document);

        if (extractedText == null || extractedText.isBlank()){
            extractedText = structuredContent;
        }

        if (extractedText == null || extractedText.isBlank()){
            extractedText = cleanText(description);
        }

        if (extractedText == null || extractedText.isBlank()){
            throw new ExtractionException(
                    "The page returned no readable text or public metadata; it may require sign-in or JavaScript rendering"
            );
        }

        int wordCount=countWords(extractedText);

        String contentType=detectContentType(
                document,
                extractedText
        );

        return ExtractedPage.builder()
                .title(title)
                .description(description)
                .canonicalUrl(canonicalUrl)
                .contentType(contentType)
                .language(language)
                .extractedText(extractedText)
                .wordCount(wordCount)
                .build();
    }

    private String extractTitle(Document document){

        String title=firstNonBlank(
                getMetaContent(document ,"og:title"),
                getMetaContent(document, "twitter:title"),
                document.title()
        );

        return limit(cleanText(title),MAX_TITLE_LENGTH);
    }

    private String extractDescription(Document document){

        String description=firstNonBlank(
                    getMetaContent(document, "description"),
                getMetaContent(document, "og:description"),
                getMetaContent(document, "twitter:description")
        );

        return  limit(cleanText(description), MAX_DESCRIPTION_LENGTH);
    }

    private String buildSafetyAssessment(Document document, String finalUrl) {
        java.util.List<String> notes = new java.util.ArrayList<>();
        try {
            java.net.URI uri = java.net.URI.create(finalUrl);
            if ("https".equalsIgnoreCase(uri.getScheme())) {
                notes.add("Connection uses HTTPS.");
            } else {
                notes.add("Connection does not use HTTPS; data may be transmitted insecurely.");
            }
        } catch (Exception ignored) {
            notes.add("Could not determine connection scheme.");
        }

        org.jsoup.select.Elements scripts = document.select("script[src]");
        int trackingScripts = 0;
        for (org.jsoup.nodes.Element script : scripts) {
            String src = script.attr("src").toLowerCase(java.util.Locale.ROOT);
            if (src.contains("google-analytics") || src.contains("googletagmanager") ||
                    src.contains("facebook.net") || src.contains("doubleclick") ||
                    src.contains("hotjar") || src.contains("segment.com")) {
                trackingScripts++;
            }
        }
        if (trackingScripts > 0) {
            notes.add("Detected " + trackingScripts + " known tracking/analytics script(s).");
        }

        String host = "";
        try { if (finalUrl != null) host = new java.net.URI(finalUrl).getHost(); } catch (Exception ignored) {}
        int externalIframes = 0;
        for (org.jsoup.nodes.Element iframe : document.select("iframe[src]")) {
            String src = iframe.attr("src");
            if (src.startsWith("http") && (host == null || host.isBlank() || !src.contains(host))) {
                externalIframes++;
            }
        }
        if (externalIframes > 0) {
            notes.add("Embedded " + externalIframes + " external iframe(s).");
        }

        if (finalUrl != null && finalUrl.startsWith("https://")) {
            int insecureResources = document.select("img[src^=http:],script[src^=http:],link[href^=http:]").size();
            if (insecureResources > 0) {
                notes.add("Page references " + insecureResources + " insecure HTTP resource(s) (mixed content).");
            }
        }

        String lowerText = (document.text() == null ? "" : document.text()).toLowerCase(java.util.Locale.ROOT);
        java.util.List<String> sensitive = java.util.List.of("password", "credit card", "ssn", "social security", "bank account");
        java.util.List<String> found = new java.util.ArrayList<>();
        for (String term : sensitive) {
            if (lowerText.contains(term)) found.add(term);
        }
        if (!found.isEmpty()) {
            notes.add("Page mentions sensitive terms: " + String.join(", ", found) + ".");
        }

        if (notes.isEmpty()) {
            return "";
        }
        return String.join(" ", notes);
    }

    private String extractCanonicalUrl(Document document){
        Element canonical=document.selectFirst(
                "link[rel=canonical][href]"
        );

        if(canonical==null){
            return null;
        }
        String href=canonical.attr("href").trim();

        if (href.isBlank()){
            return null;
        }

        try{
            URI resloved =URI.create(document.location()).resolve(href);

            if (!"http".equalsIgnoreCase(resloved.getScheme())&&
                    !"https".equalsIgnoreCase(resloved.getScheme())){
                return null;
            }
            if (resloved.getHost()==null||
                resloved.getRawUserInfo()!=null){
                return null;
            }

            return resloved.toString();
        }
        catch (IllegalArgumentException e){
            return null;
        }
    }

    private String extractLanguage(Document document){
        Element html=document.selectFirst("html");

        if(html==null){
            return null;
        }
        String language=html.attr("lang").trim();

        if (language.isBlank()){
            return null;
        }

        return limit(language,20);
    }

    private String extractMainContent(Document document){

        for(String selector:REMOVE_SELECTORS){
            document.select(selector).remove();
        }

        document.select( "[class*=advert], [id*=advert], " +
                "[class*=cookie], [id*=cookie], " +
                "[class*=social], [id*=social], " +
                "[class*=sidebar], [id*=sidebar], " +
                "[class*=popup], [id*=popup], " +
                "[class*=newsletter], [id*=newsletter]"
        ).remove();

        Element main=document.selectFirst("article, main, [role=main]");

        String text;

        if (main!=null){
            text = main.text();
        }
        else {
            Element body=document.body();
            text= body.text();
        }

        text=cleanText(text);

        return limit(text,MAX_TEXT_LENGTH);
    }

    private String extractStructuredContent(Document document) {
        Set<String> fragments = new LinkedHashSet<>();
        for (Element script : document.select("script[type=application/ld+json]")) {
            try {
                appendStructuredText(JSON_MAPPER.readTree(script.data()), fragments);
            } catch (JacksonException e) {
                log.debug("Skipping malformed JSON-LD structured metadata");
            }
        }
        return limit(cleanText(String.join(" ", fragments)), MAX_TEXT_LENGTH);
    }

    private void appendStructuredText(JsonNode node, Set<String> fragments) {
        if (node == null) {
            return;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                appendStructuredText(child, fragments);
            }
            return;
        }
        if (!node.isObject()) {
            return;
        }
        for (var field : node.properties()) {
            if (STRUCTURED_CONTENT_FIELDS.contains(field.getKey()) && field.getValue().isTextual()) {
                String value = cleanText(field.getValue().asString());
                if (value != null && !value.isBlank()) {
                    fragments.add(value);
                }
            } else {
                appendStructuredText(field.getValue(), fragments);
            }
        }
    }

    private String detectContentType(Document document, String extractedText){
        String ogType=getMetaContent(document, "og:type");

        if (ogType!=null){
            String normalized= ogType.toLowerCase(Locale.ROOT);

            if (normalized.contains("article")){
                return "ARTICLE";
            }
            if (normalized.contains("product")){
                return "PRODUCT";
            }
            if (normalized.contains("video")){
                return "VIDEO";
            }
        }

        if (document.selectFirst("article")!=null){
            return "ARTICLE";
        }

        if (document.selectFirst("pre code, .highlight, .codehilite")!=null){
            return "DOCUMENTATION";
        }

        if (document.selectFirst("[itemtype*=schema.org/product]")!=null){
            return "PRODUCT";
        }

        if (extractedText.length()<500){
            return "GENERAL";
        }
        return "WEBPAGE";
    }

    private String getMetaContent(Document document, String name){

        Element element=document
                .selectFirst("meta[name="+name+"], "+
                        "meta[property="+name+"]"
                );
        if (element==null){
            return null;
        }
        return element.attr("content").trim();
    }

    private String firstNonBlank(String... values){
        for (String value:values){
            if (value!=null&&!value.isBlank()){
                return value;
            }
        }
        return null;
    }

    private String cleanText(String text){

        if (text==null){
            return null;
        }

        return WHITESPACE.matcher(text)
                .replaceAll(" ")
                .trim();
    }

    private String limit(String text,int max){
        if (text==null ||text.length()<=max){
            return text;
        }
        return text.substring(0, max);
    }

    private int countWords(String text){
        if (text==null ||text.isBlank()){
            return 0;
        }
        return text.trim().split("\\s+").length;
    }

    private String detectCharset(SafeWebFetcher.FetchedPage fetchedPage){
        return "UTF-8";
    }
}
