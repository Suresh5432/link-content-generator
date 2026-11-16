package com.linklens.extractionservice.repo;

import com.linklens.extractionservice.entity.ExtractionOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ExtractionOutboxRepository extends JpaRepository<ExtractionOutboxEvent, UUID> {
}
