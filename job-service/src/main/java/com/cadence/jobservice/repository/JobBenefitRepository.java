package com.cadence.jobservice.repository;

import com.cadence.jobservice.entity.JobBenefit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JobBenefitRepository extends JpaRepository<JobBenefit, UUID> {
    List<JobBenefit> findAllByJobId(UUID jobId);
    void deleteAllByJobId(UUID jobId);
}
