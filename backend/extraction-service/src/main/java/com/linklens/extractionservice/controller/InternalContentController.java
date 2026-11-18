package com.linklens.extractionservice.controller;

import com.linklens.extractionservice.dto.ExtractedContentResponse;
import com.linklens.extractionservice.service.InternalContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/contents")
public class InternalContentController {

    private final InternalContentService internalContentService;

    @GetMapping("/{contentId")
    public ResponseEntity<ExtractedContentResponse> getContentById(
            @PathVariable UUID contentId) {
        return ResponseEntity.ok(internalContentService.getContent(contentId));
    }
}
