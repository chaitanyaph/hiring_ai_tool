package com.cadence.codingassessmentservice.repository;

import com.cadence.codingassessmentservice.constants.ProgrammingLanguage;
import com.cadence.codingassessmentservice.entity.QuestionStarterCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuestionStarterCodeRepository extends JpaRepository<QuestionStarterCode, UUID> {
    List<QuestionStarterCode> findAllByQuestionId(UUID questionId);
    Optional<QuestionStarterCode> findByQuestionIdAndLanguage(UUID questionId, ProgrammingLanguage language);
    void deleteAllByQuestionId(UUID questionId);
}
