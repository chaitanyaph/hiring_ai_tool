package com.cadence.aiinterviewservice.repository;

import com.cadence.aiinterviewservice.entity.InterviewScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface InterviewScoreRepository extends JpaRepository<InterviewScore, UUID> {
    Optional<InterviewScore> findBySessionId(UUID sessionId);
    void deleteBySessionId(UUID sessionId);

    @Query("SELECT AVG(sc.overallScore) FROM InterviewScore sc, InterviewSession s " +
            "WHERE sc.sessionId = s.id AND s.jobId = :jobId AND s.status = 'COMPLETED'")
    Double findAvgOverallScoreByJob(@Param("jobId") UUID jobId);

    @Query("SELECT AVG(sc.communicationScore) FROM InterviewScore sc, InterviewSession s " +
            "WHERE sc.sessionId = s.id AND s.jobId = :jobId AND s.status = 'COMPLETED'")
    Double findAvgCommunicationScoreByJob(@Param("jobId") UUID jobId);

    @Query("SELECT COUNT(sc) FROM InterviewScore sc, InterviewSession s " +
            "WHERE sc.sessionId = s.id AND s.jobId = :jobId AND s.status = 'COMPLETED' AND sc.confidenceScore < :threshold")
    long countFlaggedForReviewByJob(@Param("jobId") UUID jobId, @Param("threshold") int threshold);
}
