package com.cadence.codingassessmentservice.service;

import com.cadence.codingassessmentservice.constants.Difficulty;
import com.cadence.codingassessmentservice.dto.request.CreateQuestionRequest;
import com.cadence.codingassessmentservice.dto.request.UpdateQuestionRequest;
import com.cadence.codingassessmentservice.dto.response.PagedResponse;
import com.cadence.codingassessmentservice.dto.response.QuestionResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/** The reusable question bank -- field shape confirmed by the seeded mockup data, no dedicated creation screen is exported (see README "Architecture Decisions"). */
public interface QuestionService {

    QuestionResponse createQuestion(UUID companyId, CreateQuestionRequest request);

    QuestionResponse updateQuestion(UUID companyId, UUID questionId, UpdateQuestionRequest request);

    void deleteQuestion(UUID companyId, UUID questionId);

    QuestionResponse getQuestion(UUID companyId, UUID questionId);

    PagedResponse<QuestionResponse> listQuestions(UUID companyId, Difficulty difficulty, Pageable pageable);
}
