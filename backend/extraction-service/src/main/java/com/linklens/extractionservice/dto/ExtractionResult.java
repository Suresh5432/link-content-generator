package com.linklens.extractionservice.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ExtractionResult {
    private String finalUrl;
    private String responseContentType;
    private ExtractedPage page;
}
