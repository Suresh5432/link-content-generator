package com.linklens.extractionservice.service;

import com.linklens.extractionservice.config.ExtractionProperties;
import com.linklens.extractionservice.exception.ExtractionException;
import com.linklens.extractionservice.exception.RetryableExtractionException;
import com.linklens.extractionservice.exception.UnsafeUrlException;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
@Slf4j
public class SafeWebFetcher {

    private final UrlSafetyValidator urlSafetyValidator;

    private final ExtractionProperties extractionProperties;


    private final HttpClient httpClient;

    public SafeWebFetcher(UrlSafetyValidator urlSafetyValidator, ExtractionProperties extractionProperties,
                          ExtractionProperties properties) {
        this.urlSafetyValidator = urlSafetyValidator;
        this.extractionProperties = extractionProperties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(
                        properties.getConnectTimeoutSeconds()))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public FetchedPage fetch(String rawUrl){

        URI currentUri=urlSafetyValidator.validate(rawUrl);

        for (int redirectCount=0;
             redirectCount<= extractionProperties.getMaxRedirects();
             redirectCount++){

            HttpResponse<InputStream> response = sendRequest(currentUri);

            int statusCode=response.statusCode();

            if (isRedirect(statusCode)){
                log.debug("Received HTTP redirect {} while fetching webpage", statusCode);
                closeBody(response.body());

                String location=response
                        .headers()
                        .firstValue("Location")
                        .orElseThrow(()->
                                new ExtractionException("Redirect without location header"));
                if ((redirectCount==extractionProperties.getMaxRedirects())){
                    throw new ExtractionException("Too many redirects");
                }

                try{
                    URI redirectUri=currentUri.resolve(location);

                    currentUri=urlSafetyValidator
                            .validate(redirectUri.toString());
                }
                catch (IllegalArgumentException ex){
                    throw new UnsafeUrlException("Invalid redirect URL");
                }
                continue;
            }
            if (statusCode==429||statusCode>=500){
                log.warn("Target website returned retryable HTTP status {}", statusCode);
                closeBody(response.body());
                throw new RetryableExtractionException("The website is temporarily unavailable");
            }

            if(statusCode<200||statusCode>=300){
                log.warn("Target website returned unsuccessful HTTP status {}", statusCode);
                closeBody(response.body());
                throw new ExtractionException("WebSite returned HTTP status "+ statusCode);
            }

            String contentType=response
                    .headers()
                    .firstValue("Content-Type")
                    .orElse("");

            String normalizedContentType=contentType.toLowerCase();

            if(!normalizedContentType.contains("text/html")
                    && !normalizedContentType.contains("application/xhtml+xml")){
                closeBody(response.body());
                throw new ExtractionException("URL does not point to an HTML page");
            }

            long contentLength=response
                    .headers()
                    .firstValueAsLong("Content-Length")
                    .orElse(-1L);

            if (contentLength> extractionProperties.getMaxResponseSizeBytes()){
                closeBody(response.body());
                throw new ExtractionException("webpage is too large");
            }

            byte[] body=readLimited(response.body());

            return new FetchedPage(
                    currentUri.toString(),
                    contentType,
                    body
            );
        }
        throw new ExtractionException("Unable to fetch webpage");
    }

    private HttpResponse<InputStream> sendRequest(URI uri){

        HttpRequest request=HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", "LinkContextGenerator/1.0")
                .headers("Accept", "text/html, appliction/xhtml+xml")
                .headers("Accept-Encoding","en-US, en;q=0.9")
                .GET()
                .build();

        try{
            return httpClient.send(request,HttpResponse.BodyHandlers.ofInputStream());
        }
        catch (InterruptedException exception){
            Thread.currentThread().interrupt();
            log.warn("Webpage request was interrupted", exception);
            throw new RetryableExtractionException("Webpage fetching was interrupted");
        }
        catch (IOException exception){
            log.warn("Unable to connect to target website", exception);
            throw new RetryableExtractionException("Unable to connect to the target website");
        }
    }

    private byte[] readLimited(InputStream inputStream) {

        int maxBytes=extractionProperties.getMaxResponseSizeBytes();

        try(InputStream input =inputStream;
            ByteArrayOutputStream output=new ByteArrayOutputStream()) {

            byte[] buffer=new byte[8192];
            int totalBytes=0;
            int bytesRead;
            while((bytesRead= input.read(buffer))!=-1){

                totalBytes+=bytesRead;

                if(totalBytes>maxBytes){
                    throw new ExtractionException("Webpage exceeds maximum size");
                }

                output.write(buffer,0,bytesRead);
            }
            return output.toByteArray();
        }
        catch (IOException e) {
            log.warn("Failed while reading webpage response", e);
            throw new RetryableExtractionException("Failed while reading webpage content");
        }
    }

    private boolean isRedirect(int statusCode){
        return statusCode ==301 ||
                statusCode ==302 ||
                statusCode==303 ||
                statusCode==307 ||
                statusCode==308;
    }

    private void closeBody(InputStream inputStream){
        try{
            inputStream.close();
        }
        catch (IOException exception){

        }
    }

    public record FetchedPage(
            String finalUrl,
            String contentType,
            byte[] body
    ){}

}
