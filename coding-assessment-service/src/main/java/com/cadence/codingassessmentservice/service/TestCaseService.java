package com.cadence.codingassessmentservice.service;

import com.cadence.codingassessmentservice.dto.request.BulkImportTestCasesRequest;
import com.cadence.codingassessmentservice.dto.request.CreateTestCaseRequest;
import com.cadence.codingassessmentservice.dto.request.ReorderTestCasesRequest;
import com.cadence.codingassessmentservice.dto.response.QuestionResponse;

import java.util.List;
import java.util.UUID;

/** Incremental test-case management for a single question -- add/edit/delete/duplicate/reorder/bulk-import one row at a time, unlike Question{Create,Update}Request's destructive full-replace. */
public interface TestCaseService {

    List<QuestionResponse.TestCaseResponse> listTestCases(UUID companyId, UUID questionId);

    QuestionResponse.TestCaseResponse addTestCase(UUID companyId, UUID questionId, CreateTestCaseRequest request);

    QuestionResponse.TestCaseResponse updateTestCase(UUID companyId, UUID questionId, UUID testCaseId, CreateTestCaseRequest request);

    void deleteTestCase(UUID companyId, UUID questionId, UUID testCaseId);

    QuestionResponse.TestCaseResponse duplicateTestCase(UUID companyId, UUID questionId, UUID testCaseId);

    void reorderTestCases(UUID companyId, UUID questionId, ReorderTestCasesRequest request);

    List<QuestionResponse.TestCaseResponse> bulkImport(UUID companyId, UUID questionId, BulkImportTestCasesRequest request);
}
