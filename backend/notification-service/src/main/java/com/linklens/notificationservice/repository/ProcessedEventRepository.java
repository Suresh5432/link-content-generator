package com.linklens.notificationservice.repository;


import com.linklens.notificationservice.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID>{
    boolean existsByEventId(UUID eventId);

    @Modifying
    @Query(value = "insert into processed_events(event_id,event_type,processed_at) values (:eventId,:eventType,current_timestamp) on conflict (event_id) do nothing", nativeQuery = true)
    int claimEvent(@Param("eventId") UUID eventId, @Param("eventType") String eventType);
}
