package com.cadence.codingassessmentservice.repository;

import com.cadence.codingassessmentservice.entity.CandidateQuestionProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CandidateQuestionProgressRepository extends JpaRepository<CandidateQuestionProgress, UUID> {
    List<CandidateQuestionProgress> findAllByCandidateAssessmentIdOrderByDisplayOrderAsc(UUID candidateAssessmentId);
    Optional<CandidateQuestionProgress> findByCandidateAssessmentIdAndQuestionId(UUID candidateAssessmentId, UUID questionId);
}
