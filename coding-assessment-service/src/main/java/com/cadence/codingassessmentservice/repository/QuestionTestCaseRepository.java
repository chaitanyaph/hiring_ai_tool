package com.cadence.codingassessmentservice.repository;

import com.cadence.codingassessmentservice.constants.TestCaseVisibility;
import com.cadence.codingassessmentservice.entity.QuestionTestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface QuestionTestCaseRepository extends JpaRepository<QuestionTestCase, UUID> {
    List<QuestionTestCase> findAllByQuestionIdOrderByDisplayOrderAsc(UUID questionId);
    List<QuestionTestCase> findAllByQuestionIdAndVisibilityOrderByDisplayOrderAsc(UUID questionId, TestCaseVisibility visibility);
    long countByQuestionIdAndVisibility(UUID questionId, TestCaseVisibility visibility);
    void deleteAllByQuestionId(UUID questionId);

    @Query("SELECT tc.questionId AS questionId, COUNT(tc) AS count FROM QuestionTestCase tc WHERE tc.questionId IN :questionIds GROUP BY tc.questionId")
    List<QuestionIdCount> countByQuestionIdIn(@Param("questionIds") List<UUID> questionIds);
}
