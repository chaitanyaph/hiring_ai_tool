package com.cadence.codingassessmentservice.service.impl;

import com.cadence.codingassessmentservice.constants.Difficulty;
import com.cadence.codingassessmentservice.constants.TestCaseVisibility;
import com.cadence.codingassessmentservice.dto.request.CreateQuestionRequest;
import com.cadence.codingassessmentservice.dto.request.UpdateQuestionRequest;
import com.cadence.codingassessmentservice.dto.response.PagedResponse;
import com.cadence.codingassessmentservice.dto.response.QuestionResponse;
import com.cadence.codingassessmentservice.entity.Question;
import com.cadence.codingassessmentservice.entity.QuestionStarterCode;
import com.cadence.codingassessmentservice.entity.QuestionTestCase;
import com.cadence.codingassessmentservice.exception.ErrorCode;
import com.cadence.codingassessmentservice.exception.ResourceNotFoundException;
import com.cadence.codingassessmentservice.mapper.QuestionMapper;
import com.cadence.codingassessmentservice.repository.QuestionRepository;
import com.cadence.codingassessmentservice.repository.QuestionStarterCodeRepository;
import com.cadence.codingassessmentservice.repository.QuestionTestCaseRepository;
import com.cadence.codingassessmentservice.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private final QuestionRepository questionRepository;
    private final QuestionStarterCodeRepository starterCodeRepository;
    private final QuestionTestCaseRepository testCaseRepository;
    private final QuestionMapper questionMapper;

    @Override
    @Transactional
    public QuestionResponse createQuestion(UUID companyId, CreateQuestionRequest request) {
        Question question = Question.builder()
                .companyId(companyId)
                .title(request.getTitle())
                .difficulty(request.getDifficulty())
                .marks(request.getMarks())
                .description(request.getDescription())
                .exampleText(request.getExampleText())
                .constraintsText(request.getConstraintsText())
                .tags(questionMapper.joinCsv(request.getTags()))
                .allowedLanguages(questionMapper.joinCsv(request.getAllowedLanguages()))
                .build();
        question = questionRepository.save(question);

        persistChildren(question.getId(), request.getStarterCodes(), request.getTestCases());

        return getQuestion(companyId, question.getId());
    }

    @Override
    @Transactional
    public QuestionResponse updateQuestion(UUID companyId, UUID questionId, UpdateQuestionRequest request) {
        Question question = findByIdAndCompanyOrThrow(companyId, questionId);
        question.setTitle(request.getTitle());
        question.setDifficulty(request.getDifficulty());
        question.setMarks(request.getMarks());
        question.setDescription(request.getDescription());
        question.setExampleText(request.getExampleText());
        question.setConstraintsText(request.getConstraintsText());
        question.setTags(questionMapper.joinCsv(request.getTags()));
        question.setAllowedLanguages(questionMapper.joinCsv(request.getAllowedLanguages()));
        questionRepository.save(question);

        // Same "recalculate fully replaces children" precedent used across the platform.
        starterCodeRepository.deleteAllByQuestionId(questionId);
        testCaseRepository.deleteAllByQuestionId(questionId);
        persistChildren(questionId, request.getStarterCodes(), request.getTestCases());

        return getQuestion(companyId, questionId);
    }

    @Override
    @Transactional
    public void deleteQuestion(UUID companyId, UUID questionId) {
        Question question = findByIdAndCompanyOrThrow(companyId, questionId);
        questionRepository.delete(question);
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionResponse getQuestion(UUID companyId, UUID questionId) {
        Question question = findByIdAndCompanyOrThrow(companyId, questionId);
        QuestionResponse response = questionMapper.toResponse(question);

        Map<String, String> starterCodes = new LinkedHashMap<>();
        for (QuestionStarterCode sc : starterCodeRepository.findAllByQuestionId(questionId)) {
            starterCodes.put(sc.getLanguage().name(), sc.getCode());
        }
        response.setStarterCodes(starterCodes);

        List<QuestionTestCase> testCases = testCaseRepository.findAllByQuestionIdOrderByDisplayOrderAsc(questionId);
        response.setTestCases(questionMapper.toTestCaseResponseList(testCases));
        response.setHiddenTestCaseCount((int) testCaseRepository.countByQuestionIdAndVisibility(questionId, TestCaseVisibility.HIDDEN));

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<QuestionResponse> listQuestions(UUID companyId, Difficulty difficulty, Pageable pageable) {
        Page<Question> page = questionRepository.search(companyId, difficulty, pageable);
        return PagedResponse.from(page.map(questionMapper::toResponse));
    }

    private void persistChildren(UUID questionId, List<CreateQuestionRequest.StarterCodeItem> starterCodes,
                                  List<CreateQuestionRequest.TestCaseItem> testCases) {
        if (starterCodes != null) {
            List<QuestionStarterCode> entities = starterCodes.stream()
                    .map(sc -> QuestionStarterCode.builder().questionId(questionId).language(sc.getLanguage()).code(sc.getCode()).build())
                    .toList();
            starterCodeRepository.saveAll(entities);
        }
        if (testCases != null) {
            List<QuestionTestCase> entities = new java.util.ArrayList<>();
            int order = 0;
            for (CreateQuestionRequest.TestCaseItem tc : testCases) {
                entities.add(QuestionTestCase.builder()
                        .questionId(questionId).visibility(tc.getVisibility())
                        .inputData(tc.getInputData()).expectedOutput(tc.getExpectedOutput())
                        .displayOrder(order++).build());
            }
            testCaseRepository.saveAll(entities);
        }
    }

    private Question findByIdAndCompanyOrThrow(UUID companyId, UUID questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.QUESTION_NOT_FOUND, "Question not found: " + questionId));
        if (!question.getCompanyId().equals(companyId)) {
            throw new ResourceNotFoundException(ErrorCode.QUESTION_NOT_FOUND, "Question not found: " + questionId);
        }
        return question;
    }
}
