package com.linklens.extractionservice.websiteanalysis.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "website_findings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WebsiteFinding {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "analysis_id", nullable = false)
    private UUID analysisId;

    @Column(name = "page_id")
    private UUID pageId;

    @Column(nullable = false, length = 60)
    private String type;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String evidence;

    @Column(length = 20)
    private String confidence;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = Instant.now();
    }
}
