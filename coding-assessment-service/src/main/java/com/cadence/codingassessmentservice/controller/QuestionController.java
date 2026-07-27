package com.cadence.codingassessmentservice.controller;

import com.cadence.codingassessmentservice.constants.Difficulty;
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
    @Operation(summary = "Delete a question")
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
    @Operation(summary = "List questions in the bank")
    public ResponseEntity<ApiResponse<PagedResponse<QuestionResponse>>> list(
            @RequestParam(required = false) Difficulty difficulty, @PageableDefault(size = 20) Pageable pageable) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        return ResponseEntity.ok(ApiResponse.ok("OK", questionService.listQuestions(companyId, difficulty, pageable)));
    }
}
