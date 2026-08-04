package com.cadence.codingassessmentservice.controller;

import com.cadence.codingassessmentservice.dto.request.BulkImportTestCasesRequest;
import com.cadence.codingassessmentservice.dto.request.CreateTestCaseRequest;
import com.cadence.codingassessmentservice.dto.request.ReorderTestCasesRequest;
import com.cadence.codingassessmentservice.dto.response.ApiResponse;
import com.cadence.codingassessmentservice.dto.response.QuestionResponse;
import com.cadence.codingassessmentservice.security.CurrentUserProvider;
import com.cadence.codingassessmentservice.service.TestCaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Incremental test-case management for a single question, separate from the full-replace-on-save behavior of question create/update. */
@RestController
@RequestMapping("/api/v1/questions/{questionId}/test-cases")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','HIRING_MANAGER','ADMIN')")
@Tag(name = "Question Test Cases", description = "Add/edit/delete/duplicate/reorder/bulk-import test cases for a single question")
public class TestCaseController {

    private final TestCaseService testCaseService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    @Operation(summary = "List all test cases for a question")
    public ResponseEntity<ApiResponse<List<QuestionResponse.TestCaseResponse>>> list(@PathVariable UUID questionId) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        return ResponseEntity.ok(ApiResponse.ok("OK", testCaseService.listTestCases(companyId, questionId)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Add a test case")
    public ResponseEntity<ApiResponse<QuestionResponse.TestCaseResponse>> add(@PathVariable UUID questionId, @Valid @RequestBody CreateTestCaseRequest request) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        return ResponseEntity.ok(ApiResponse.ok("Test case added", testCaseService.addTestCase(companyId, questionId, request)));
    }

    @PutMapping("/{testCaseId}")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Update a test case")
    public ResponseEntity<ApiResponse<QuestionResponse.TestCaseResponse>> update(
            @PathVariable UUID questionId, @PathVariable UUID testCaseId, @Valid @RequestBody CreateTestCaseRequest request) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        return ResponseEntity.ok(ApiResponse.ok("Test case updated", testCaseService.updateTestCase(companyId, questionId, testCaseId, request)));
    }

    @DeleteMapping("/{testCaseId}")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Delete a test case")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID questionId, @PathVariable UUID testCaseId) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        testCaseService.deleteTestCase(companyId, questionId, testCaseId);
        return ResponseEntity.ok(ApiResponse.ok("Test case deleted"));
    }

    @PostMapping("/{testCaseId}/duplicate")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Duplicate a test case")
    public ResponseEntity<ApiResponse<QuestionResponse.TestCaseResponse>> duplicate(@PathVariable UUID questionId, @PathVariable UUID testCaseId) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        return ResponseEntity.ok(ApiResponse.ok("Test case duplicated", testCaseService.duplicateTestCase(companyId, questionId, testCaseId)));
    }

    @PutMapping("/reorder")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Reorder test cases")
    public ResponseEntity<ApiResponse<Void>> reorder(@PathVariable UUID questionId, @Valid @RequestBody ReorderTestCasesRequest request) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        testCaseService.reorderTestCases(companyId, questionId, request);
        return ResponseEntity.ok(ApiResponse.ok("Test cases reordered"));
    }

    @PostMapping("/bulk-import")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Bulk-import test cases", description = "Appends to any existing test cases rather than replacing them")
    public ResponseEntity<ApiResponse<List<QuestionResponse.TestCaseResponse>>> bulkImport(
            @PathVariable UUID questionId, @Valid @RequestBody BulkImportTestCasesRequest request) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        return ResponseEntity.ok(ApiResponse.ok("Test cases imported", testCaseService.bulkImport(companyId, questionId, request)));
    }
}
