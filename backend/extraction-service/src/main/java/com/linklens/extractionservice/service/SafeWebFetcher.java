package com.linklens.extractionservice.service;

import com.linklens.extractionservice.config.ExtractionProperties;
import com.linklens.extractionservice.exception.ExtractionException;
import com.linklens.extractionservice.exception.RetryableExtractionException;
import com.linklens.extractionservice.exception.UnsafeUrlException;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.time.Duration;

@Component
@Primary
@Slf4j
public class SafeWebFetcher {
    private final UrlSafetyValidator validator;
    private final ExtractionProperties properties;

    public SafeWebFetcher(UrlSafetyValidator validator, ExtractionProperties properties) {
        this.validator = validator;
        this.properties = properties;
    }

    public FetchedPage fetchRaw(String rawUrl) {
        URI uri = validator.validate(rawUrl);
        for (int redirects = 0; redirects <= properties.getMaxRedirects(); redirects++) {
            CloseableHttpClient client = clientPinnedTo(uri);
            try (client; CloseableHttpResponse response = client.execute(classicRequest(uri))) {
                int status = response.getCode();
                if (isRedirect(status)) {
                    if (redirects == properties.getMaxRedirects()) throw new ExtractionException("Too many redirects");
                    var location = response.getFirstHeader("Location");
                    if (location == null || location.getValue().isBlank()) throw new ExtractionException("Redirect without location header");
                    try { uri = validator.validate(uri.resolve(location.getValue()).toString()); }
                    catch (IllegalArgumentException e) { throw new UnsafeUrlException("Invalid redirect URL"); }
                    continue;
                }
                if (status == 429 || status >= 500) throw new RetryableExtractionException("The website is temporarily unavailable");
                if ((status < 200 || status >= 300) && status != 404) throw new ExtractionException("Website returned HTTP status " + status);
                var entity = response.getEntity();
                if (entity == null && status != 404) throw new ExtractionException("Webpage response was empty");
                long declaredLength = entity == null ? 0 : entity.getContentLength();
                if (declaredLength > properties.getMaxResponseSizeBytes()) throw new ExtractionException("Webpage is too large");
                byte[] body = entity == null ? new byte[0] : readLimited(entity.getContent());
                String contentType = response.getFirstHeader("Content-Type") == null ? "" : response.getFirstHeader("Content-Type").getValue();
                java.util.Map<String,String> headers = new java.util.LinkedHashMap<>();
                for (org.apache.hc.core5.http.Header h : response.getHeaders()) headers.put(h.getName(), h.getValue());
                return new FetchedPage(uri.toString(), contentType, body, headers, status);
            } catch (UnsafeUrlException | ExtractionException | RetryableExtractionException e) {
                throw e;
            } catch (IOException e) {
                log.warn("Unable to fetch resource", e);
                throw new RetryableExtractionException("Unable to connect to the target website");
            }
        }
        throw new ExtractionException("Unable to fetch resource");
    }

    public FetchedPage fetch(String rawUrl) {
        URI uri = validator.validate(rawUrl);
        for (int redirects = 0; redirects <= properties.getMaxRedirects(); redirects++) {
            CloseableHttpClient client = clientPinnedTo(uri);
            try (client; CloseableHttpResponse response = client.execute(
                    classicRequest(uri))) {
                int status = response.getCode();
                if (isRedirect(status)) {
                    if (redirects == properties.getMaxRedirects()) throw new ExtractionException("Too many redirects");
                    var location = response.getFirstHeader("Location");
                    if (location == null || location.getValue().isBlank()) throw new ExtractionException("Redirect without location header");
                    try { uri = validator.validate(uri.resolve(location.getValue()).toString()); }
                    catch (IllegalArgumentException e) { throw new UnsafeUrlException("Invalid redirect URL"); }
                    continue;
                }
                if (status == 429 || status >= 500) throw new RetryableExtractionException("The website is temporarily unavailable");
                if (status < 200 || status >= 300) throw new ExtractionException("Website returned HTTP status " + status);
                var entity = response.getEntity();
                String contentType = response.getFirstHeader("Content-Type") == null ? "" : response.getFirstHeader("Content-Type").getValue();
                String normalizedType = contentType.toLowerCase(java.util.Locale.ROOT);
                if (!normalizedType.contains("text/html") && !normalizedType.contains("application/xhtml+xml")) throw new ExtractionException("URL does not point to an HTML page");
                if (entity == null) throw new ExtractionException("Webpage response was empty");
                long declaredLength = entity.getContentLength();
                if (declaredLength > properties.getMaxResponseSizeBytes()) throw new ExtractionException("Webpage is too large");
                byte[] body = readLimited(entity.getContent());
                java.util.Map<String,String> headers = new java.util.LinkedHashMap<>();
                for (org.apache.hc.core5.http.Header h : response.getHeaders()) {
                    headers.put(h.getName(), h.getValue());
                }
                return new FetchedPage(uri.toString(), contentType, body, headers, status);
            } catch (UnsafeUrlException | ExtractionException | RetryableExtractionException e) {
                throw e;
            } catch (IOException e) {
                log.warn("Unable to fetch webpage", e);
                throw new RetryableExtractionException("Unable to connect to the target website");
            }
        }
        throw new ExtractionException("Unable to fetch webpage");
    }

    private CloseableHttpClient clientPinnedTo(URI uri) {
        final InetAddress[] approved = validator.resolvePublicAddresses(uri.getHost());
        DnsResolver resolver = new DnsResolver() {
            @Override public InetAddress[] resolve(String host) throws UnknownHostException {
                if (!host.equalsIgnoreCase(uri.getHost())) throw new UnknownHostException("Unexpected host");
                return approved.clone();
            }
            @Override public String resolveCanonicalHostname(String host) throws UnknownHostException {
                if (!host.equalsIgnoreCase(uri.getHost())) throw new UnknownHostException("Unexpected host");
                return host;
            }
        };
        var manager = PoolingHttpClientConnectionManagerBuilder.create().setDnsResolver(resolver).build();
        var requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofSeconds(properties.getConnectTimeoutSeconds()))
                .setResponseTimeout(Timeout.ofSeconds(properties.getRequestTimeoutSeconds())).build();
        return HttpClients.custom().setConnectionManager(manager).setDefaultRequestConfig(requestConfig)
                .disableRedirectHandling().disableAutomaticRetries().disableContentCompression().build();
    }

    private org.apache.hc.core5.http.ClassicHttpRequest classicRequest(URI uri) {
        var request = new org.apache.hc.core5.http.message.BasicClassicHttpRequest("GET", uri);
        request.setHeader("User-Agent", properties.getUserAgent());
        request.setHeader("Accept", "text/html, application/xhtml+xml");
        request.setHeader("Accept-Encoding", "identity");
        return request;
    }

    private byte[] readLimited(java.io.InputStream input) throws IOException {
        try (input; var output = new java.io.ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int total = 0, read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > properties.getMaxResponseSizeBytes()) throw new ExtractionException("Webpage exceeds maximum size");
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }

    private boolean isRedirect(int status) { return status == 301 || status == 302 || status == 303 || status == 307 || status == 308; }
    public record FetchedPage(
            String finalUrl,
            String contentType,
            byte[] body,
            java.util.Map<String, String> headers,
            int statusCode
    ) {
        public FetchedPage(String finalUrl, String contentType, byte[] body, java.util.Map<String, String> headers) {
            this(finalUrl, contentType, body, headers, 200);
        }
    }
}
