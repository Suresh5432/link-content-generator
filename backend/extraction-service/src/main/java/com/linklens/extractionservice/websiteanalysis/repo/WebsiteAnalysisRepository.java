package com.linklens.extractionservice.websiteanalysis.repo;

import com.linklens.extractionservice.websiteanalysis.entity.WebsiteAnalysis;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WebsiteAnalysisRepository extends JpaRepository<WebsiteAnalysis, UUID> {
    Page<WebsiteAnalysis> findAllByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    Optional<WebsiteAnalysis> findByIdAndUserId(UUID id, String userId);

    List<WebsiteAnalysis> findAllByUserId(String userId);

    long deleteByUserId(String userId);
}
