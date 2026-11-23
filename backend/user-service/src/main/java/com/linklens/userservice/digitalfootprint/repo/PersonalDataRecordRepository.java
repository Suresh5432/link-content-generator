package com.linklens.userservice.digitalfootprint.repo;

import com.linklens.userservice.digitalfootprint.entity.PersonalDataRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersonalDataRecordRepository extends JpaRepository<PersonalDataRecord, UUID> {
    List<PersonalDataRecord> findAllByTrackedWebsiteIdAndUserIdOrderByCreatedAtDesc(UUID websiteId, String userId);
    List<PersonalDataRecord> findAllByUserIdOrderByCreatedAtDesc(String userId);
    Optional<PersonalDataRecord> findByIdAndUserId(UUID id, String userId);
    long countByUserId(String userId);
}
