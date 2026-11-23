package com.linklens.extractionservice.websiteanalysis.repo;

import com.linklens.extractionservice.websiteanalysis.entity.WebsiteFinding;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface WebsiteFindingRepository extends JpaRepository<WebsiteFinding, UUID> {
    List<WebsiteFinding> findAllByAnalysisId(UUID analysisId);
}
