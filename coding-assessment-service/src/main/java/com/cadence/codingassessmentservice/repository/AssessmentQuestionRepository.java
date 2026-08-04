package com.cadence.codingassessmentservice.repository;

import com.cadence.codingassessmentservice.entity.AssessmentQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssessmentQuestionRepository extends JpaRepository<AssessmentQuestion, UUID> {
    List<AssessmentQuestion> findAllByAssessmentIdOrderByDisplayOrderAsc(UUID assessmentId);
    void deleteAllByAssessmentId(UUID assessmentId);
    long countByAssessmentId(UUID assessmentId);
    long countByQuestionId(UUID questionId);
}
