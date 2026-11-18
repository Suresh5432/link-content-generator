package com.linklens.summarizationservice.repo;

import com.linklens.summarizationservice.entity.SummaryOutboxEvent;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface SummaryOutboxRepository extends JpaRepository<SummaryOutboxEvent, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT e from SummaryOutboxEvent e
            where e.publishedAt is null
                and e.availableAt <=:now
            order by e.createdAt ASC 
""")
    List<SummaryOutboxEvent> findPendingEvents(@Param("now")Instant now,
                                               Pageable pageable);
}
