package com.linklens.linkservice.repo;

import com.linklens.linkservice.entity.Link;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LinkRepository extends JpaRepository<Link, UUID> {

    Optional<Link> findByIdAndUserIdAndDeletedAtIsNull(UUID id, String userId);

    Page<Link> findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(String userId, Pageable pageable);

    boolean existsByUserIdAndIdempotencyKey(String userId, String idempotencyKey);

    Optional<Link> findByUserIdAndIdempotencyKey(String userId, String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l from Link l where l.id=:id")
    Optional<Link> findByIdForUpdate(@Param("id") UUID id);
}
