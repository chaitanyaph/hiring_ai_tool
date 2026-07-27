package com.cadence.jobservice.repository;

import com.cadence.jobservice.entity.JobAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JobAuditRepository extends JpaRepository<JobAudit, UUID> {
    List<JobAudit> findAllByJobIdOrderByPerformedAtDesc(UUID jobId);
}
