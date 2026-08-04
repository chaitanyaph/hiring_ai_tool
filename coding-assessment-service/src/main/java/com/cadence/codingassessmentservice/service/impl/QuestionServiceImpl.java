package com.cadence.codingassessmentservice.service.impl;

import com.cadence.codingassessmentservice.constants.Difficulty;
import com.cadence.codingassessmentservice.constants.QuestionStatus;
import com.cadence.codingassessmentservice.constants.TestCaseVisibility;
import com.cadence.codingassessmentservice.dto.request.CreateQuestionRequest;
import com.cadence.codingassessmentservice.dto.request.UpdateQuestionRequest;
import com.cadence.codingassessmentservice.dto.response.PagedResponse;
import com.cadence.codingassessmentservice.dto.response.QuestionResponse;
import com.cadence.codingassessmentservice.entity.Question;
import com.cadence.codingassessmentservice.entity.QuestionHint;
import com.cadence.codingassessmentservice.entity.QuestionStarterCode;
import com.cadence.codingassessmentservice.entity.QuestionTestCase;
import com.cadence.codingassessmentservice.exception.AssessmentConflictException;
import com.cadence.codingassessmentservice.exception.ErrorCode;
import com.cadence.codingassessmentservice.exception.ResourceNotFoundException;
import com.cadence.codingassessmentservice.mapper.QuestionMapper;
import com.cadence.codingassessmentservice.repository.AssessmentQuestionRepository;
import com.cadence.codingassessmentservice.repository.QuestionHintRepository;
import com.cadence.codingassessmentservice.repository.QuestionRepository;
import com.cadence.codingassessmentservice.repository.QuestionStarterCodeRepository;
import com.cadence.codingassessmentservice.repository.QuestionTestCaseRepository;
import com.cadence.codingassessmentservice.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
    private final QuestionHintRepository questionHintRepository;
    private final AssessmentQuestionRepository assessmentQuestionRepository;
    private final QuestionMapper questionMapper;

    @Override
    @Transactional
    public QuestionResponse createQuestion(UUID companyId, CreateQuestionRequest request) {
        Question question = Question.builder()
                .companyId(companyId)
                .title(request.getTitle())
                .status(request.isActivateNow() ? QuestionStatus.ACTIVE : QuestionStatus.DRAFT)
                .difficulty(request.getDifficulty())
                .marks(request.getMarks())
                .description(request.getDescription())
                .exampleText(request.getExampleText())
                .constraintsText(request.getConstraintsText())
                .inputFormat(request.getInputFormat())
                .outputFormat(request.getOutputFormat())
                .explanation(request.getExplanation())
                .tags(questionMapper.joinCsv(request.getTags()))
                .topics(questionMapper.joinCsv(request.getTopics()))
                .allowedLanguages(questionMapper.joinCsv(request.getAllowedLanguages()))
                .timeLimitMs(request.getTimeLimitMs() != null ? request.getTimeLimitMs() : 2000)
                .memoryLimitMb(request.getMemoryLimitMb() != null ? request.getMemoryLimitMb() : 256)
                .build();
        question = questionRepository.save(question);

        persistChildren(question.getId(), request.getStarterCodes(), request.getTestCases(), request.getHints());

        return getQuestion(companyId, question.getId());
    }

    @Override
    @Transactional
    public QuestionResponse updateQuestion(UUID companyId, UUID questionId, UpdateQuestionRequest request) {
        Question question = findByIdAndCompanyOrThrow(companyId, questionId);
        if (question.getStatus() == QuestionStatus.ARCHIVED) {
            throw new AssessmentConflictException(ErrorCode.QUESTION_ARCHIVED, "An archived question must be reactivated before it can be edited");
        }
        question.setTitle(request.getTitle());
        question.setDifficulty(request.getDifficulty());
        question.setMarks(request.getMarks());
        question.setDescription(request.getDescription());
        question.setExampleText(request.getExampleText());
        question.setConstraintsText(request.getConstraintsText());
        question.setInputFormat(request.getInputFormat());
        question.setOutputFormat(request.getOutputFormat());
        question.setExplanation(request.getExplanation());
        question.setTags(questionMapper.joinCsv(request.getTags()));
        question.setTopics(questionMapper.joinCsv(request.getTopics()));
        question.setAllowedLanguages(questionMapper.joinCsv(request.getAllowedLanguages()));
        question.setTimeLimitMs(request.getTimeLimitMs() != null ? request.getTimeLimitMs() : question.getTimeLimitMs());
        question.setMemoryLimitMb(request.getMemoryLimitMb() != null ? request.getMemoryLimitMb() : question.getMemoryLimitMb());
        questionRepository.save(question);

        // Same "recalculate fully replaces children" precedent used across the platform.
        starterCodeRepository.deleteAllByQuestionId(questionId);
        testCaseRepository.deleteAllByQuestionId(questionId);
        questionHintRepository.deleteAllByQuestionId(questionId);
        persistChildren(questionId, request.getStarterCodes(), request.getTestCases(), request.getHints());

        return getQuestion(companyId, questionId);
    }

    @Override
    @Transactional
    public void deleteQuestion(UUID companyId, UUID questionId) {
        Question question = findByIdAndCompanyOrThrow(companyId, questionId);
        if (assessmentQuestionRepository.countByQuestionId(questionId) > 0) {
            throw new AssessmentConflictException(ErrorCode.QUESTION_IN_USE,
                    "This question is used in one or more assessments -- archive it instead of deleting");
        }
        questionRepository.delete(question);
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionResponse getQuestion(UUID companyId, UUID questionId) {
        Question question = findByIdAndCompanyOrThrow(companyId, questionId);
        return toFullResponse(question);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<QuestionResponse> listQuestions(UUID companyId, Difficulty difficulty, QuestionStatus status, String keyword, Pageable pageable) {
        Page<Question> page = questionRepository.search(companyId, difficulty, status, keyword == null || keyword.isBlank() ? null : keyword.trim(), pageable);
        return PagedResponse.from(page.map(questionMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionResponse> listActiveQuestions(UUID companyId) {
        return questionRepository.findAllActiveByCompanyId(companyId).stream().map(questionMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public QuestionResponse duplicateQuestion(UUID companyId, UUID questionId) {
        Question source = findByIdAndCompanyOrThrow(companyId, questionId);
        Question copy = Question.builder()
                .companyId(companyId)
                .title(source.getTitle() + " (Copy)")
                .status(QuestionStatus.DRAFT)
                .difficulty(source.getDifficulty())
                .marks(source.getMarks())
                .description(source.getDescription())
                .exampleText(source.getExampleText())
                .constraintsText(source.getConstraintsText())
                .inputFormat(source.getInputFormat())
                .outputFormat(source.getOutputFormat())
                .explanation(source.getExplanation())
                .tags(source.getTags())
                .topics(source.getTopics())
                .allowedLanguages(source.getAllowedLanguages())
                .timeLimitMs(source.getTimeLimitMs())
                .memoryLimitMb(source.getMemoryLimitMb())
                .build();
        copy = questionRepository.save(copy);

        for (QuestionStarterCode sc : starterCodeRepository.findAllByQuestionId(source.getId())) {
            starterCodeRepository.save(QuestionStarterCode.builder().questionId(copy.getId()).language(sc.getLanguage()).code(sc.getCode()).build());
        }
        for (QuestionTestCase tc : testCaseRepository.findAllByQuestionIdOrderByDisplayOrderAsc(source.getId())) {
            testCaseRepository.save(QuestionTestCase.builder().questionId(copy.getId()).visibility(tc.getVisibility())
                    .inputData(tc.getInputData()).expectedOutput(tc.getExpectedOutput()).explanation(tc.getExplanation())
                    .weight(tc.getWeight()).displayOrder(tc.getDisplayOrder()).build());
        }
        for (QuestionHint hint : questionHintRepository.findAllByQuestionIdOrderByDisplayOrderAsc(source.getId())) {
            questionHintRepository.save(QuestionHint.builder().questionId(copy.getId()).hintText(hint.getHintText()).displayOrder(hint.getDisplayOrder()).build());
        }

        return getQuestion(companyId, copy.getId());
    }

    @Override
    @Transactional
    public QuestionResponse activateQuestion(UUID companyId, UUID questionId) {
        Question question = findByIdAndCompanyOrThrow(companyId, questionId);
        question.setStatus(QuestionStatus.ACTIVE);
        questionRepository.save(question);
        return toFullResponse(question);
    }

    @Override
    @Transactional
    public QuestionResponse deactivateQuestion(UUID companyId, UUID questionId) {
        Question question = findByIdAndCompanyOrThrow(companyId, questionId);
        if (question.getStatus() == QuestionStatus.ARCHIVED) {
            throw new AssessmentConflictException(ErrorCode.QUESTION_ARCHIVED, "An archived question must be reactivated first");
        }
        question.setStatus(QuestionStatus.INACTIVE);
        questionRepository.save(question);
        return toFullResponse(question);
    }

    @Override
    @Transactional
    public QuestionResponse archiveQuestion(UUID companyId, UUID questionId) {
        Question question = findByIdAndCompanyOrThrow(companyId, questionId);
        question.setStatus(QuestionStatus.ARCHIVED);
        questionRepository.save(question);
        return toFullResponse(question);
    }

    private QuestionResponse toFullResponse(Question question) {
        QuestionResponse response = questionMapper.toResponse(question);

        Map<String, String> starterCodes = new LinkedHashMap<>();
        for (QuestionStarterCode sc : starterCodeRepository.findAllByQuestionId(question.getId())) {
            starterCodes.put(sc.getLanguage().name(), sc.getCode());
        }
        response.setStarterCodes(starterCodes);

        List<QuestionTestCase> testCases = testCaseRepository.findAllByQuestionIdOrderByDisplayOrderAsc(question.getId());
        response.setTestCases(questionMapper.toTestCaseResponseList(testCases));
        response.setHiddenTestCaseCount((int) testCaseRepository.countByQuestionIdAndVisibility(question.getId(), TestCaseVisibility.HIDDEN));

        response.setHints(questionHintRepository.findAllByQuestionIdOrderByDisplayOrderAsc(question.getId()).stream()
                .map(QuestionHint::getHintText).toList());

        response.setUsedInAssessmentCount((int) assessmentQuestionRepository.countByQuestionId(question.getId()));

        return response;
    }

    private void persistChildren(UUID questionId, List<CreateQuestionRequest.StarterCodeItem> starterCodes,
                                  List<CreateQuestionRequest.TestCaseItem> testCases, List<String> hints) {
        if (starterCodes != null) {
            List<QuestionStarterCode> entities = starterCodes.stream()
                    .map(sc -> QuestionStarterCode.builder().questionId(questionId).language(sc.getLanguage()).code(sc.getCode()).build())
                    .toList();
            starterCodeRepository.saveAll(entities);
        }
        if (testCases != null) {
            List<QuestionTestCase> entities = new ArrayList<>();
            int order = 0;
            for (CreateQuestionRequest.TestCaseItem tc : testCases) {
                entities.add(QuestionTestCase.builder()
                        .questionId(questionId).visibility(tc.getVisibility())
                        .inputData(tc.getInputData()).expectedOutput(tc.getExpectedOutput())
                        .explanation(tc.getExplanation()).weight(tc.getWeight() != null ? tc.getWeight() : 1)
                        .displayOrder(order++).build());
            }
            testCaseRepository.saveAll(entities);
        }
        if (hints != null) {
            List<QuestionHint> entities = new ArrayList<>();
            int order = 0;
            for (String hint : hints) {
                entities.add(QuestionHint.builder().questionId(questionId).hintText(hint).displayOrder(order++).build());
            }
            questionHintRepository.saveAll(entities);
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
