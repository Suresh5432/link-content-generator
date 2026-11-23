package com.linklens.extractionservice.websiteanalysis.entity;

import com.linklens.extractionservice.websiteanalysis.WebsiteAnalysisStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "website_analyses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WebsiteAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "original_url", nullable = false, columnDefinition = "TEXT")
    private String originalUrl;

    @Column(name = "normalized_url", nullable = false, columnDefinition = "TEXT")
    private String normalizedUrl;

    @Column(name = "domain")
    private String domain;

    @Column(name = "title")
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private WebsiteAnalysisStatus status;

    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    @Column(name = "confidence")
    private Integer confidence;

    @Column(name = "category")
    private String category;

    @Column(name = "report", columnDefinition = "TEXT")
    private String report;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    public void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (status == null) status = WebsiteAnalysisStatus.QUEUED;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = Instant.now();
    }
}
