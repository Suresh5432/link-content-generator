package com.linklens.extractionservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "extracted_contents",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_extracted_contents_link",
                        columnNames = "link_id"
                )
        },
        indexes = {
                @Index(
                        name = "idx_extracted_contents_user",
                        columnList = "user_id, created_at"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtractionContent {

    @Id
    private UUID id;

    @Column(name = "link_id", nullable = false)
    private UUID linkId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "source_url", nullable = false,columnDefinition = "TEXT")
    private String sourceUrl;

    @Column(name = "canonical_url",columnDefinition = "TEXT")
    private String canonicalUrl;

    @Column(length = 1000)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(length = 20)
    private String language;

    @Column(name="extracted_text", nullable = false,columnDefinition = "TEXT")
    private String extractedText;

    @Column(name="word_count", nullable = false)
    private int wordCount;

    @Column(name = "fetched_at" , nullable = false)
    private Instant fetchAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    @PreUpdate
    protected void onUpdate(){
        this.updatedAt = Instant.now();
    }


}
