package com.cadence.applicationservice.repository;

import com.cadence.applicationservice.constant.ScoreType;
import com.cadence.applicationservice.entity.ApplicationScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApplicationScoreRepository extends JpaRepository<ApplicationScore, UUID> {
    List<ApplicationScore> findAllByApplicationIdOrderByRecordedAtAsc(UUID applicationId);
    List<ApplicationScore> findAllByApplicationIdAndScoreTypeOrderByRecordedAtDesc(UUID applicationId, ScoreType scoreType);
}
