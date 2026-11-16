package com.linklens.extractionservice.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ExtractionResult {
    private String finalUr;
    private String responseContentType;
    private ExtractedPage page;
}
