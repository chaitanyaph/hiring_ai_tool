package com.cadence.codingassessmentservice.repository;

import com.cadence.codingassessmentservice.entity.AssessmentEligibility;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssessmentEligibilityRepository extends JpaRepository<AssessmentEligibility, UUID> {
    Optional<AssessmentEligibility> findByApplicationId(UUID applicationId);
    List<AssessmentEligibility> findAllByJobId(UUID jobId);
}
