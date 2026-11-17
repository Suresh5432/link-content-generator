package com.linklens.summarizationservice.repo;

import com.linklens.summarizationservice.entity.SummaryOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SummaryOutboxRepository extends JpaRepository<SummaryOutboxEvent, UUID> {
}
