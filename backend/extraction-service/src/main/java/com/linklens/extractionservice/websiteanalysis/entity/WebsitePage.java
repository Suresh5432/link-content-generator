package com.linklens.extractionservice.websiteanalysis.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "website_pages", uniqueConstraints = @UniqueConstraint(columnNames = {"analysis_id", "url"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WebsitePage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "analysis_id", nullable = false)
    private UUID analysisId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(name = "canonical_url", columnDefinition = "TEXT")
    private String canonicalUrl;

    @Column
    private String title;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "content_type")
    private String contentType;

    @Column
    private Integer depth;

    @Column(name = "safety_notes", columnDefinition = "TEXT")
    private String safetyNotes;

    @Column(name = "content_excerpt", columnDefinition = "TEXT")
    private String contentExcerpt;

    @Column(name = "fetched_at")
    private Instant fetchedAt;
}
