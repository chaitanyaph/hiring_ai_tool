package com.cadence.aiinterviewservice.repository;

import com.cadence.aiinterviewservice.constants.InterviewSessionStatus;
import com.cadence.aiinterviewservice.entity.InterviewSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InterviewSessionRepository extends JpaRepository<InterviewSession, UUID> {

    Optional<InterviewSession> findByApplicationId(UUID applicationId);

    @Query("""
            SELECT s FROM InterviewSession s
            WHERE s.jobId = :jobId
            AND (:status IS NULL OR s.status = :status)
            """)
    Page<InterviewSession> search(@Param("jobId") UUID jobId, @Param("status") InterviewSessionStatus status, Pageable pageable);

    List<InterviewSession> findAllByJobIdAndStatus(UUID jobId, InterviewSessionStatus status);

    Page<InterviewSession> findAllByJobIdAndStatusOrderByCompletedAtDesc(UUID jobId, InterviewSessionStatus status, Pageable pageable);

    long countByJobIdAndStatus(UUID jobId, InterviewSessionStatus status);

    long countByJobIdAndStatusAndCompletedAtAfter(UUID jobId, InterviewSessionStatus status, LocalDateTime after);

    List<InterviewSession> findAllByStatusAndExpiresAtBefore(InterviewSessionStatus status, LocalDateTime now);
}
