package com.cadence.codingassessmentservice.repository;

import com.cadence.codingassessmentservice.entity.QuestionHint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuestionHintRepository extends JpaRepository<QuestionHint, UUID> {
    List<QuestionHint> findAllByQuestionIdOrderByDisplayOrderAsc(UUID questionId);
    void deleteAllByQuestionId(UUID questionId);
}
