package com.cadence.jobservice.repository;

import com.cadence.jobservice.constant.JobAssignmentRole;
import com.cadence.jobservice.entity.JobAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobAssignmentRepository extends JpaRepository<JobAssignment, UUID> {
    List<JobAssignment> findAllByJobId(UUID jobId);
    Optional<JobAssignment> findByJobIdAndAssignmentRole(UUID jobId, JobAssignmentRole role);
    void deleteByJobIdAndAssignmentRole(UUID jobId, JobAssignmentRole role);
}
