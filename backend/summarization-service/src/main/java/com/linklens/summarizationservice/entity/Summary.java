package com.linklens.summarizationservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "summaries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Summary {
    @Id
    private UUID id;

    @Column(name = "link_id", nullable = false, unique = true)
    private UUID linkId;

    @Column(name = "content_id", nullable = false)
    private UUID contentId;

    @Column(name = "canonical_url", columnDefinition = "TEXT")
    private String canonicalUrl;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(length = 1000)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name="key_takeaways", nullable = false, columnDefinition = "jsonb")
    private List<String> keyTakeaways;

    @Column(name = "reading_time_minutes", nullable = false)
    private int readingTimeMinutes;

    @Column(name = "word_count", nullable = false)
    private int wordCount;

    @Column(name = "ai_provider", nullable = false)
    private String aiProvider;

    @Column(name = "ai_model")
    private String aiModel;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

}
