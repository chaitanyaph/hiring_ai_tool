package com.cadence.jobservice.repository;

import com.cadence.jobservice.entity.JobPipelineStage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JobPipelineStageRepository extends JpaRepository<JobPipelineStage, UUID> {
    List<JobPipelineStage> findAllByJobIdOrderByStageOrderAsc(UUID jobId);
    void deleteAllByJobId(UUID jobId);
}
