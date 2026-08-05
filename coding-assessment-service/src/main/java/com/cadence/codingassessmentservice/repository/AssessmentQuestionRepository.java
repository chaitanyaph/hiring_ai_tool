package com.cadence.codingassessmentservice.repository;

import com.cadence.codingassessmentservice.entity.AssessmentQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AssessmentQuestionRepository extends JpaRepository<AssessmentQuestion, UUID> {
    List<AssessmentQuestion> findAllByAssessmentIdOrderByDisplayOrderAsc(UUID assessmentId);
    void deleteAllByAssessmentId(UUID assessmentId);
    long countByAssessmentId(UUID assessmentId);
    long countByQuestionId(UUID questionId);

    @Query("SELECT aq.questionId AS questionId, COUNT(DISTINCT aq.assessmentId) AS count FROM AssessmentQuestion aq WHERE aq.questionId IN :questionIds GROUP BY aq.questionId")
    List<QuestionIdCount> countDistinctAssessmentsByQuestionIdIn(@Param("questionIds") List<UUID> questionIds);
}
