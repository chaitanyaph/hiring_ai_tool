package com.cadence.jobservice.repository;

import com.cadence.jobservice.entity.JobStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JobStatusHistoryRepository extends JpaRepository<JobStatusHistory, UUID> {
    List<JobStatusHistory> findAllByJobIdOrderByChangedAtDesc(UUID jobId);
}
