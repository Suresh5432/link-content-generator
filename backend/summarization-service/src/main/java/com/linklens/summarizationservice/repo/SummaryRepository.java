package com.linklens.summarizationservice.repo;

import com.linklens.summarizationservice.entity.Summary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SummaryRepository extends JpaRepository<Summary, UUID> {

    Optional<Summary> findByLinkId(UUID linkId);

    Optional<Summary> findByLinkIdAndUserId(UUID linkId,String userId);
}
