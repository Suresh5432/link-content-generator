package com.linklens.userservice.digitalfootprint.service;

import tools.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GoogleEmailProviderClient implements GoogleEmailProvider {
    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final String GMAIL_API = "https://gmail.googleapis.com/gmail/v1";
    private static final String REVOKE_ENDPOINT = "https://oauth2.googleapis.com/revoke";
    private static final Pattern EMAIL_DOMAIN = Pattern.compile("@([A-Za-z0-9.-]+)");
    private static final Set<String> IGNORED_SENDER_DOMAINS = Set.of(
            "google.com", "googlemail.com", "gmail.com", "microsoft.com", "live.com",
            "outlook.com", "hotmail.com", "yahoo.com", "icloud.com", "apple.com",
            "sendgrid.net", "mailchimp.com", "amazonses.com"
    );
    private static final Pattern REGISTRATION_SUBJECT = Pattern.compile(
            "\\b(welcome|verify|verification|confirm|confirmation|registration|account created|sign.?up)\\b",
            Pattern.CASE_INSENSITIVE
    );

    private final RestClient restClient = RestClient.create();
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public GoogleEmailProviderClient(
            @Value("${digital-footprint.connected-email.google.client-id:}") String clientId,
            @Value("${digital-footprint.connected-email.google.client-secret:}") String clientSecret,
            @Value("${digital-footprint.connected-email.google.redirect-uri:}") String redirectUri
    ) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    public boolean isConfigured() {
        return hasText(clientId) && hasText(clientSecret) && hasText(redirectUri);
    }

    public String authorizationUrl(String state, String challenge) {
        if (!isConfigured()) throw new GoogleEmailProviderException("Gmail discovery is not configured in this deployment.");
        return UriComponentsBuilder.fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "openid email https://www.googleapis.com/auth/gmail.metadata")
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .queryParam("include_granted_scopes", "false")
                .queryParam("state", state)
                .queryParam("code_challenge", challenge)
                .queryParam("code_challenge_method", "S256")
                .build()
                .encode()
                .toUriString();
    }

    @Override
    public OAuthTokens exchangeCode(String code, String verifier) {
        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("code", code);
        form.add("code_verifier", verifier);
        form.add("redirect_uri", redirectUri);
        form.add("grant_type", "authorization_code");
        JsonNode response = postForm(TOKEN_ENDPOINT, form);
        return parseTokens(response);
    }

    @Override
    public OAuthTokens refreshAccessToken(String refreshToken) {
        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("refresh_token", refreshToken);
        form.add("grant_type", "refresh_token");
        JsonNode response = postForm(TOKEN_ENDPOINT, form);
        return parseTokens(response);
    }

    @Override
    public String mailboxEmail(String accessToken) {
        JsonNode response = get(GMAIL_API + "/users/me/profile", accessToken);
        String email = response.path("emailAddress").asText("");
        if (email.isBlank()) throw new GoogleEmailProviderException("Google did not return the authorized mailbox address.");
        return email;
    }

    @Override
    public MailboxScanResult recentRegistrationMessages(String accessToken) {
        JsonNode list = get(GMAIL_API + "/users/me/messages?maxResults=100", accessToken);
        JsonNode messages = list.path("messages");
        List<MailboxMessageMetadata> results = new ArrayList<>();
        if (!messages.isArray()) return new MailboxScanResult(0, results);
        int reviewed = 0;
        for (JsonNode message : messages) {
            String id = message.path("id").asText("");
            if (id.isBlank()) continue;
            reviewed++;
            URI uri = UriComponentsBuilder.fromUriString(GMAIL_API + "/users/me/messages/{id}")
                    .queryParam("format", "metadata")
                    .queryParam("metadataHeaders", "From")
                    .queryParam("metadataHeaders", "Subject")
                    .queryParam("metadataHeaders", "Date")
                    .buildAndExpand(id)
                    .encode()
                    .toUri();
            JsonNode metadata = get(uri, accessToken);
            Map<String, String> headers = new HashMap<>();
            JsonNode headerList = metadata.path("payload").path("headers");
            if (headerList.isArray()) {
                for (JsonNode header : headerList) {
                    headers.put(header.path("name").asText("").toLowerCase(Locale.ROOT), header.path("value").asText(""));
                }
            }
            String category = subjectCategory(headers.get("subject"));
            if (category == null) continue;
            String domain = senderDomain(headers.get("from"));
            if (domain == null || isEmailProviderDomain(domain)) continue;
            results.add(new MailboxMessageMetadata(domain, category, parseDate(headers.get("date"))));
        }
        return new MailboxScanResult(reviewed, results);
    }

    @Override
    public boolean revoke(String refreshToken) {
        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("token", refreshToken);
        try {
            restClient.post().uri(REVOKE_ENDPOINT).contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form).retrieve().toBodilessEntity();
            return true;
        } catch (RestClientException exception) {
            return false;
        }
    }

    private JsonNode postForm(String endpoint, LinkedMultiValueMap<String, String> form) {
        try {
            JsonNode response = restClient.post().uri(endpoint).contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form).retrieve().body(JsonNode.class);
            if (response == null) throw new GoogleEmailProviderException("Google returned an empty OAuth response.");
            return response;
        } catch (RestClientException exception) {
            throw new GoogleEmailProviderException("Google OAuth request failed. Check provider configuration and consent.", exception);
        }
    }

    private JsonNode get(String endpoint, String accessToken) {
        return get(URI.create(endpoint), accessToken);
    }

    private JsonNode get(URI endpoint, String accessToken) {
        try {
            JsonNode response = restClient.get().uri(endpoint).headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve().body(JsonNode.class);
            if (response == null) throw new GoogleEmailProviderException("Google returned an empty mailbox response.");
            return response;
        } catch (RestClientException exception) {
            throw new GoogleEmailProviderException("Google mailbox request failed. Reconnect Gmail and try again.", exception);
        }
    }

    private OAuthTokens parseTokens(JsonNode response) {
        String accessToken = response.path("access_token").asText("");
        if (accessToken.isBlank()) throw new GoogleEmailProviderException("Google did not issue an access token.");
        return new OAuthTokens(accessToken, response.path("refresh_token").asText(null),
                response.path("scope").asText(""));
    }

    private static String senderDomain(String from) {
        if (from == null) return null;
        Matcher matcher = EMAIL_DOMAIN.matcher(from);
        String domain = null;
        while (matcher.find()) domain = matcher.group(1);
        if (domain == null) return null;
        String normalized = domain.toLowerCase(Locale.ROOT).replaceAll("\\.+$", "");
        return normalized.matches("(?=.{1,253}$)([a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,63}") ? normalized : null;
    }

    private static boolean isEmailProviderDomain(String domain) {
        return IGNORED_SENDER_DOMAINS.stream().anyMatch(ignored -> domain.equals(ignored) || domain.endsWith("." + ignored));
    }

    private static String subjectCategory(String subject) {
        if (subject == null || !REGISTRATION_SUBJECT.matcher(subject).find()) return null;
        String lower = subject.toLowerCase(Locale.ROOT);
        if (lower.contains("verif") || lower.contains("confirm")) return "verification";
        if (lower.contains("welcome")) return "welcome";
        if (lower.contains("registration") || lower.contains("sign up") || lower.contains("signup")) return "registration";
        return "account";
    }

    private static Instant parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
