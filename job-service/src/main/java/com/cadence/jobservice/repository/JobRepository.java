package com.cadence.jobservice.repository;

import com.cadence.jobservice.constant.JobStatus;
import com.cadence.jobservice.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID>, JpaSpecificationExecutor<Job> {
    boolean existsByCompanyIdAndJobCode(UUID companyId, String jobCode);
    long countByCompanyIdAndStatus(UUID companyId, JobStatus status);
    long countByCompanyId(UUID companyId);

    @Query("SELECT COUNT(DISTINCT j.departmentId) FROM Job j WHERE j.companyId = :companyId")
    long countDistinctDepartments(@Param("companyId") UUID companyId);

    List<Job> findTop5ByCompanyIdOrderByCreatedAtDesc(UUID companyId);

    List<Job> findAllByCompanyIdAndStatusInAndApplicationDeadlineBetween(
            UUID companyId, List<JobStatus> statuses, LocalDate from, LocalDate to);

    List<Job> findAllByStatusInAndApplicationDeadlineBefore(List<JobStatus> statuses, LocalDate cutoff);
}
