package com.linklens.extractionservice.repo;

import com.linklens.extractionservice.entity.ExtractionOutboxEvent;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExtractionOutboxRepository extends JpaRepository<ExtractionOutboxEvent, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(value = """
            SELECT * 
            from extraction_outbox_events
            where published_at is null
                and available_at<= current_timestamp 
            order by created_at
            limit :batchSize
            For update Skip locked
            """, nativeQuery = true)
    List<ExtractionOutboxEvent> findPendingBatch(@Param("batchSize") int batchSize);
}
