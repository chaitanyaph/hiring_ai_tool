package com.cadence.codingassessmentservice.service.impl;

import com.cadence.codingassessmentservice.dto.request.BulkImportTestCasesRequest;
import com.cadence.codingassessmentservice.dto.request.CreateTestCaseRequest;
import com.cadence.codingassessmentservice.dto.request.ReorderTestCasesRequest;
import com.cadence.codingassessmentservice.dto.response.QuestionResponse;
import com.cadence.codingassessmentservice.entity.Question;
import com.cadence.codingassessmentservice.entity.QuestionTestCase;
import com.cadence.codingassessmentservice.exception.ErrorCode;
import com.cadence.codingassessmentservice.exception.ResourceNotFoundException;
import com.cadence.codingassessmentservice.mapper.QuestionMapper;
import com.cadence.codingassessmentservice.repository.QuestionRepository;
import com.cadence.codingassessmentservice.repository.QuestionTestCaseRepository;
import com.cadence.codingassessmentservice.service.TestCaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TestCaseServiceImpl implements TestCaseService {

    private final QuestionRepository questionRepository;
    private final QuestionTestCaseRepository testCaseRepository;
    private final QuestionMapper questionMapper;

    @Override
    @Transactional(readOnly = true)
    public List<QuestionResponse.TestCaseResponse> listTestCases(UUID companyId, UUID questionId) {
        findQuestionOrThrow(companyId, questionId);
        return questionMapper.toTestCaseResponseList(testCaseRepository.findAllByQuestionIdOrderByDisplayOrderAsc(questionId));
    }

    @Override
    @Transactional
    public QuestionResponse.TestCaseResponse addTestCase(UUID companyId, UUID questionId, CreateTestCaseRequest request) {
        findQuestionOrThrow(companyId, questionId);
        int nextOrder = testCaseRepository.findAllByQuestionIdOrderByDisplayOrderAsc(questionId).size();
        QuestionTestCase testCase = QuestionTestCase.builder()
                .questionId(questionId)
                .visibility(request.getVisibility())
                .inputData(request.getInputData())
                .expectedOutput(request.getExpectedOutput())
                .explanation(request.getExplanation())
                .weight(request.getWeight() != null ? request.getWeight() : 1)
                .displayOrder(nextOrder)
                .build();
        return questionMapper.toTestCaseResponse(testCaseRepository.save(testCase));
    }

    @Override
    @Transactional
    public QuestionResponse.TestCaseResponse updateTestCase(UUID companyId, UUID questionId, UUID testCaseId, CreateTestCaseRequest request) {
        findQuestionOrThrow(companyId, questionId);
        QuestionTestCase testCase = findTestCaseOrThrow(questionId, testCaseId);
        testCase.setVisibility(request.getVisibility());
        testCase.setInputData(request.getInputData());
        testCase.setExpectedOutput(request.getExpectedOutput());
        testCase.setExplanation(request.getExplanation());
        testCase.setWeight(request.getWeight() != null ? request.getWeight() : testCase.getWeight());
        return questionMapper.toTestCaseResponse(testCaseRepository.save(testCase));
    }

    @Override
    @Transactional
    public void deleteTestCase(UUID companyId, UUID questionId, UUID testCaseId) {
        findQuestionOrThrow(companyId, questionId);
        QuestionTestCase testCase = findTestCaseOrThrow(questionId, testCaseId);
        testCaseRepository.delete(testCase);
        renumber(questionId);
    }

    @Override
    @Transactional
    public QuestionResponse.TestCaseResponse duplicateTestCase(UUID companyId, UUID questionId, UUID testCaseId) {
        findQuestionOrThrow(companyId, questionId);
        QuestionTestCase source = findTestCaseOrThrow(questionId, testCaseId);
        int nextOrder = testCaseRepository.findAllByQuestionIdOrderByDisplayOrderAsc(questionId).size();
        QuestionTestCase copy = QuestionTestCase.builder()
                .questionId(questionId).visibility(source.getVisibility())
                .inputData(source.getInputData()).expectedOutput(source.getExpectedOutput())
                .explanation(source.getExplanation()).weight(source.getWeight())
                .displayOrder(nextOrder).build();
        return questionMapper.toTestCaseResponse(testCaseRepository.save(copy));
    }

    @Override
    @Transactional
    public void reorderTestCases(UUID companyId, UUID questionId, ReorderTestCasesRequest request) {
        findQuestionOrThrow(companyId, questionId);
        List<QuestionTestCase> existing = testCaseRepository.findAllByQuestionIdOrderByDisplayOrderAsc(questionId);
        Map<UUID, QuestionTestCase> byId = existing.stream().collect(Collectors.toMap(QuestionTestCase::getId, tc -> tc));

        if (!byId.keySet().equals(new java.util.HashSet<>(request.getOrderedTestCaseIds()))) {
            throw new ResourceNotFoundException(ErrorCode.TEST_CASE_NOT_FOUND, "The reorder list must contain exactly this question's existing test case IDs");
        }

        int order = 0;
        for (UUID id : request.getOrderedTestCaseIds()) {
            QuestionTestCase testCase = byId.get(id);
            testCase.setDisplayOrder(order++);
            testCaseRepository.save(testCase);
        }
    }

    @Override
    @Transactional
    public List<QuestionResponse.TestCaseResponse> bulkImport(UUID companyId, UUID questionId, BulkImportTestCasesRequest request) {
        findQuestionOrThrow(companyId, questionId);
        int nextOrder = testCaseRepository.findAllByQuestionIdOrderByDisplayOrderAsc(questionId).size();
        List<QuestionTestCase> entities = new ArrayList<>();
        for (CreateTestCaseRequest item : request.getTestCases()) {
            entities.add(QuestionTestCase.builder()
                    .questionId(questionId).visibility(item.getVisibility())
                    .inputData(item.getInputData()).expectedOutput(item.getExpectedOutput())
                    .explanation(item.getExplanation()).weight(item.getWeight() != null ? item.getWeight() : 1)
                    .displayOrder(nextOrder++).build());
        }
        return questionMapper.toTestCaseResponseList(testCaseRepository.saveAll(entities));
    }

    private void renumber(UUID questionId) {
        List<QuestionTestCase> remaining = testCaseRepository.findAllByQuestionIdOrderByDisplayOrderAsc(questionId);
        int order = 0;
        for (QuestionTestCase testCase : remaining) {
            testCase.setDisplayOrder(order++);
            testCaseRepository.save(testCase);
        }
    }

    private Question findQuestionOrThrow(UUID companyId, UUID questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.QUESTION_NOT_FOUND, "Question not found: " + questionId));
        if (!question.getCompanyId().equals(companyId)) {
            throw new ResourceNotFoundException(ErrorCode.QUESTION_NOT_FOUND, "Question not found: " + questionId);
        }
        return question;
    }

    private QuestionTestCase findTestCaseOrThrow(UUID questionId, UUID testCaseId) {
        QuestionTestCase testCase = testCaseRepository.findById(testCaseId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.TEST_CASE_NOT_FOUND, "Test case not found: " + testCaseId));
        if (!testCase.getQuestionId().equals(questionId)) {
            throw new ResourceNotFoundException(ErrorCode.TEST_CASE_NOT_FOUND, "Test case not found: " + testCaseId);
        }
        return testCase;
    }
}
