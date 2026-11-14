package com.linklens.linkservice.service;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;

@Component
public class UrlNormalizer {

    private static final int MAX_URL_LENGTH=2048;

    public String normalize(String url) {

        if(url==null || url.isBlank()){
            throw new IllegalArgumentException("URL is required");
        }
        String input= url.trim();

        if(input.length()>MAX_URL_LENGTH){
            throw new IllegalArgumentException("URL is too long");
        }

        try{
            URI uri=new URI(input);

            String scheme= uri.getScheme();

            if (scheme==null || scheme.isBlank()){
                throw new IllegalArgumentException("URL must include http and https");
            }

            scheme=scheme.toLowerCase();

            if (!scheme.equals("http") && !scheme.equals("https")){
                throw new IllegalArgumentException("Only HTTP and HTTPS URLs are supported");
            }

            if(uri.getRawUserInfo()!=null){
                throw new IllegalArgumentException("URLs containing user credentials are not allowed");
            }

            if(uri.getPort()<-1|| uri.getPort()>65535){
                throw new IllegalArgumentException("Invalid URL post");
            }

            int port= uri.getPort();

            if((scheme.equals("http")&& port==80)||
                    (scheme.equals("https")&& port==443)){
                port=-1;
            }
            URI normalized=new URI(
                    scheme,
                    null,
                    uri.getHost().toLowerCase(),
                    port,
                    uri.getPath(),
                    uri.getQuery(),
                    null
            ).normalize();
            return normalized.toASCIIString();
        }
        catch (URISyntaxException ex){
            throw new IllegalArgumentException("Invalid URL format");
        }
    }
}
