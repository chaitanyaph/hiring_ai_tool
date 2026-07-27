package com.cadence.codingassessmentservice.repository;

import com.cadence.codingassessmentservice.entity.ExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExecutionLogRepository extends JpaRepository<ExecutionLog, UUID> {
    List<ExecutionLog> findAllByCandidateAssessmentIdAndQuestionIdOrderByExecutedAtDesc(UUID candidateAssessmentId, UUID questionId);
}
