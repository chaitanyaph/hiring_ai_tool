package com.cadence.codingassessmentservice.repository;

import com.cadence.codingassessmentservice.constants.TestCaseVisibility;
import com.cadence.codingassessmentservice.entity.QuestionTestCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuestionTestCaseRepository extends JpaRepository<QuestionTestCase, UUID> {
    List<QuestionTestCase> findAllByQuestionIdOrderByDisplayOrderAsc(UUID questionId);
    List<QuestionTestCase> findAllByQuestionIdAndVisibilityOrderByDisplayOrderAsc(UUID questionId, TestCaseVisibility visibility);
    long countByQuestionIdAndVisibility(UUID questionId, TestCaseVisibility visibility);
    void deleteAllByQuestionId(UUID questionId);
}
