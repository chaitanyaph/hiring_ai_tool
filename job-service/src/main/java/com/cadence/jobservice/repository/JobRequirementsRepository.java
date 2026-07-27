package com.cadence.jobservice.repository;

import com.cadence.jobservice.entity.JobRequirements;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JobRequirementsRepository extends JpaRepository<JobRequirements, UUID> {
    Optional<JobRequirements> findByJobId(UUID jobId);
}
