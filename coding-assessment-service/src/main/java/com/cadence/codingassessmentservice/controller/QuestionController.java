package com.cadence.codingassessmentservice.controller;

import com.cadence.codingassessmentservice.constants.Difficulty;
import com.cadence.codingassessmentservice.constants.QuestionStatus;
import com.cadence.codingassessmentservice.dto.request.CreateQuestionRequest;
import com.cadence.codingassessmentservice.dto.request.UpdateQuestionRequest;
import com.cadence.codingassessmentservice.dto.response.ApiResponse;
import com.cadence.codingassessmentservice.dto.response.PagedResponse;
import com.cadence.codingassessmentservice.dto.response.QuestionResponse;
import com.cadence.codingassessmentservice.security.CurrentUserProvider;
import com.cadence.codingassessmentservice.service.QuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** The reusable question bank -- field shape confirmed by the seeded mockup data (see README "Architecture Decisions" for why this has no dedicated Figma screen but is still real, required backend functionality). */
@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','HIRING_MANAGER','ADMIN')")
@Tag(name = "Questions", description = "The reusable coding-question bank")
public class QuestionController {

    private final QuestionService questionService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Create a question")
    public ResponseEntity<ApiResponse<QuestionResponse>> create(@Valid @RequestBody CreateQuestionRequest request) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        return ResponseEntity.ok(ApiResponse.ok("Question created", questionService.createQuestion(companyId, request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Update a question")
    public ResponseEntity<ApiResponse<QuestionResponse>> update(@PathVariable UUID id, @Valid @RequestBody UpdateQuestionRequest request) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        return ResponseEntity.ok(ApiResponse.ok("Question updated", questionService.updateQuestion(companyId, id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Delete a question", description = "Only permitted when the question isn't used in any assessment -- archive it instead otherwise")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        questionService.deleteQuestion(companyId, id);
        return ResponseEntity.ok(ApiResponse.ok("Question deleted"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a question")
    public ResponseEntity<ApiResponse<QuestionResponse>> get(@PathVariable UUID id) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        return ResponseEntity.ok(ApiResponse.ok("OK", questionService.getQuestion(companyId, id)));
    }

    @GetMapping
    @Operation(summary = "List/search questions in the bank")
    public ResponseEntity<ApiResponse<PagedResponse<QuestionResponse>>> list(
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(required = false) QuestionStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        return ResponseEntity.ok(ApiResponse.ok("OK", questionService.listQuestions(companyId, difficulty, status, search, pageable)));
    }

    @GetMapping("/active")
    @Operation(summary = "List every ACTIVE question", description = "Feeds the assessment builder's question picker")
    public ResponseEntity<ApiResponse<List<QuestionResponse>>> listActive() {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        return ResponseEntity.ok(ApiResponse.ok("OK", questionService.listActiveQuestions(companyId)));
    }

    @PostMapping("/{id}/duplicate")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Duplicate a question into a new draft copy")
    public ResponseEntity<ApiResponse<QuestionResponse>> duplicate(@PathVariable UUID id) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        return ResponseEntity.ok(ApiResponse.ok("Question duplicated", questionService.duplicateQuestion(companyId, id)));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Activate a question", description = "Makes it selectable in the assessment builder")
    public ResponseEntity<ApiResponse<QuestionResponse>> activate(@PathVariable UUID id) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        return ResponseEntity.ok(ApiResponse.ok("Question activated", questionService.activateQuestion(companyId, id)));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Deactivate a question", description = "Hides it from the assessment builder without deleting it")
    public ResponseEntity<ApiResponse<QuestionResponse>> deactivate(@PathVariable UUID id) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        return ResponseEntity.ok(ApiResponse.ok("Question deactivated", questionService.deactivateQuestion(companyId, id)));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Archive a question", description = "Soft-delete -- safe even when the question is used by existing assessments")
    public ResponseEntity<ApiResponse<QuestionResponse>> archive(@PathVariable UUID id) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        return ResponseEntity.ok(ApiResponse.ok("Question archived", questionService.archiveQuestion(companyId, id)));
    }
}
