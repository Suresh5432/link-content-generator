package com.linklens.extractionservice.repo;

import com.linklens.extractionservice.entity.ExtractionContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExtractionContentRepository extends JpaRepository<ExtractionContent, UUID> {

    Optional<ExtractionContent> findByLinkId(UUID linkId);

    boolean existsByLinkId(UUID linkId);
}
