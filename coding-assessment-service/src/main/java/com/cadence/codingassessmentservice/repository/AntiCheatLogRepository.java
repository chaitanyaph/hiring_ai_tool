package com.cadence.codingassessmentservice.repository;

import com.cadence.codingassessmentservice.constants.AntiCheatEventType;
import com.cadence.codingassessmentservice.entity.AntiCheatLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AntiCheatLogRepository extends JpaRepository<AntiCheatLog, UUID> {
    List<AntiCheatLog> findAllByCandidateAssessmentIdOrderByOccurredAtAsc(UUID candidateAssessmentId);
    long countByCandidateAssessmentIdAndEventType(UUID candidateAssessmentId, AntiCheatEventType eventType);
}
