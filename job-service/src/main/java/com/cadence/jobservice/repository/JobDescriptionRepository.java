package com.cadence.jobservice.repository;

import com.cadence.jobservice.entity.JobDescription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JobDescriptionRepository extends JpaRepository<JobDescription, UUID> {
    Optional<JobDescription> findByJobId(UUID jobId);
}
