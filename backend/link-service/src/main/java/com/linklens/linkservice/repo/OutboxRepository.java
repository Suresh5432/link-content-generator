package com.linklens.linkservice.repo;

import com.linklens.linkservice.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query(value= """
                SELECT *
                from outbox_events
                where published_at is null
                    And available_at <= CURRENT_TIMESTAMP
                order by created_at
                LIMIT :batchSize
                For UPDATE SKIP LOCKED
                """, nativeQuery = true)
    List<OutboxEvent> findUnpublishedBatchForUpdate(@Param("batchSize") int batchSize);
}
