package com.cadence.codingassessmentservice.repository;

import com.cadence.codingassessmentservice.constants.CandidateAssessmentStatus;
import com.cadence.codingassessmentservice.entity.CandidateAssessment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CandidateAssessmentRepository extends JpaRepository<CandidateAssessment, UUID> {

    Optional<CandidateAssessment> findByAssessmentIdAndApplicationId(UUID assessmentId, UUID applicationId);

    List<CandidateAssessment> findAllByApplicationId(UUID applicationId);

    List<CandidateAssessment> findAllByAssessmentId(UUID assessmentId);

    Page<CandidateAssessment> findAllByCandidateIdOrderByCreatedAtDesc(UUID candidateId, Pageable pageable);

    @Query("""
            SELECT ca FROM CandidateAssessment ca
            WHERE ca.assessmentId = :assessmentId
            AND (:status IS NULL OR ca.status = :status)
            """)
    Page<CandidateAssessment> search(@Param("assessmentId") UUID assessmentId, @Param("status") CandidateAssessmentStatus status, Pageable pageable);

    long countByAssessmentIdAndStatus(UUID assessmentId, CandidateAssessmentStatus status);

    long countByAssessmentIdAndStatusAndCompletedAtAfter(UUID assessmentId, CandidateAssessmentStatus status, LocalDateTime after);

    List<CandidateAssessment> findAllByAssessmentIdAndStatusOrderByTotalScoreDescTimeUsedSecondsAsc(UUID assessmentId, CandidateAssessmentStatus status);

    Page<CandidateAssessment> findAllByAssessmentIdAndStatusOrderByCompletedAtDesc(UUID assessmentId, CandidateAssessmentStatus status, Pageable pageable);

    @Query("SELECT AVG(ca.totalScore) FROM CandidateAssessment ca WHERE ca.assessmentId = :assessmentId AND ca.status = 'COMPLETED'")
    Double findAvgScoreByAssessment(@Param("assessmentId") UUID assessmentId);

    @Query("SELECT MAX(ca.totalScore) FROM CandidateAssessment ca WHERE ca.assessmentId = :assessmentId AND ca.status = 'COMPLETED'")
    Integer findMaxScoreByAssessment(@Param("assessmentId") UUID assessmentId);

    @Query("SELECT MIN(ca.totalScore) FROM CandidateAssessment ca WHERE ca.assessmentId = :assessmentId AND ca.status = 'COMPLETED'")
    Integer findMinScoreByAssessment(@Param("assessmentId") UUID assessmentId);

    List<CandidateAssessment> findAllByStatusAndExpiresAtBefore(CandidateAssessmentStatus status, LocalDateTime now);

    @Query("""
            SELECT ca FROM CandidateAssessment ca
            WHERE ca.status = :status
            AND ca.remindedAt IS NULL
            AND ca.expiresAt IS NOT NULL
            AND ca.expiresAt BETWEEN :now AND :reminderCutoff
            """)
    List<CandidateAssessment> findAllDueForReminder(@Param("status") CandidateAssessmentStatus status,
                                                      @Param("now") LocalDateTime now,
                                                      @Param("reminderCutoff") LocalDateTime reminderCutoff);
}
