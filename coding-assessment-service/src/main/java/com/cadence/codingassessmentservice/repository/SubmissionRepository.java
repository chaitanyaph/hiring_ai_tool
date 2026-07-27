package com.cadence.codingassessmentservice.repository;

import com.cadence.codingassessmentservice.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {
    List<Submission> findAllByCandidateAssessmentIdAndQuestionIdOrderBySubmittedAtDesc(UUID candidateAssessmentId, UUID questionId);
    List<Submission> findAllByCandidateAssessmentIdOrderBySubmittedAtAsc(UUID candidateAssessmentId);
    long countByCandidateAssessmentIdAndQuestionId(UUID candidateAssessmentId, UUID questionId);

    /** The latest attempt per question is what counts toward the final score. */
    Optional<Submission> findFirstByCandidateAssessmentIdAndQuestionIdOrderBySubmittedAtDesc(UUID candidateAssessmentId, UUID questionId);
}
