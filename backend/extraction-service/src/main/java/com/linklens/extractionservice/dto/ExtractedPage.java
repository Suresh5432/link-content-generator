package com.linklens.extractionservice.dto;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExtractedPage {
    private String title;
    private String description;
    private String canonicalUrl;
    private String contentType;
    private String language;
    private String extractedText;
    private int wordCount;
}
