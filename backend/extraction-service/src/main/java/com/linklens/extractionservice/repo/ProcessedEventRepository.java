package com.linklens.extractionservice.repo;

import com.linklens.extractionservice.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {

    boolean existsByEventId(UUID eventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProcessedEvent p where p.eventId = :eventId")
    java.util.Optional<ProcessedEvent> findForUpdate(@Param("eventId") UUID eventId);

    @org.springframework.data.jpa.repository.Modifying
    @Query(value = "insert into processed_events(event_id, event_type, processed_at) values (:eventId, :eventType, current_timestamp) on conflict (event_id) do nothing", nativeQuery = true)
    int claimEvent(@Param("eventId") UUID eventId, @Param("eventType") String eventType);
}
