package com.linklens.linkservice.repo;

import com.linklens.linkservice.entity.Link;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LinkRepository extends JpaRepository<Link, UUID> {
    List<Link> findAllByUserIdOrderByCreatedAtDesc(String userId);
}
