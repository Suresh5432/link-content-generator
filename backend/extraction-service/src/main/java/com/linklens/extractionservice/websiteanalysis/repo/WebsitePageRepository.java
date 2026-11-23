package com.linklens.extractionservice.websiteanalysis.repo;

import com.linklens.extractionservice.websiteanalysis.entity.WebsitePage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface WebsitePageRepository extends JpaRepository<WebsitePage, UUID> {
    List<WebsitePage> findAllByAnalysisId(UUID analysisId);
}
