package com.cadence.codingassessmentservice.repository;

import com.cadence.codingassessmentservice.constants.AssessmentStatus;
import com.cadence.codingassessmentservice.entity.Assessment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AssessmentRepository extends JpaRepository<Assessment, UUID> {

    @Query("""
            SELECT a FROM Assessment a
            WHERE a.companyId = :companyId
            AND (:status IS NULL OR a.status = :status)
            AND (:jobId IS NULL OR a.jobId = :jobId)
            """)
    Page<Assessment> search(@Param("companyId") UUID companyId, @Param("status") AssessmentStatus status,
                             @Param("jobId") UUID jobId, Pageable pageable);

    List<Assessment> findAllByJobIdAndStatus(UUID jobId, AssessmentStatus status);

    List<Assessment> findAllByJobId(UUID jobId);
}
