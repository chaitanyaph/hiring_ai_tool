package com.cadence.resumeparserservice.controller;

import com.cadence.resumeparserservice.dto.response.*;
import com.cadence.resumeparserservice.service.ResumeAnalysisQueryService;
import com.cadence.resumeparserservice.service.ResumeMatchAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Backs the Resume Analysis Dashboard, the per-job candidate ranking
 * table, the cross-job Recommendations feed, and the analysis drawer's
 * sub-sections (skills/missing-skills/strengths/weaknesses/
 * recommendation/summary). Same recruiter role set as the parsing
 * endpoints; recalculate is excluded for HIRING_MANAGER, same
 * view-only convention as retry on ParsedResumeController.
 */
@RestController
@RequestMapping("/api/v1/resume-analysis")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','HIRING_MANAGER','ADMIN')")
@Tag(name = "Resume Analysis", description = "Resume-to-job match scores, skill comparison, AI recommendation, and candidate ranking")
public class ResumeAnalysisController {

    private final ResumeAnalysisQueryService resumeAnalysisQueryService;
    private final ResumeMatchAnalysisService resumeMatchAnalysisService;

    @GetMapping("/jobs/{jobId}")
    @Operation(summary = "Candidate ranking table for a job", description = "Search matches the candidate's parsed name/email")
    public ResponseEntity<ApiResponse<PagedResponse<ResumeMatchRankingItemResponse>>> getRanking(
            @PathVariable UUID jobId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("OK", resumeAnalysisQueryService.getRanking(jobId, search, pageable)));
    }

    @GetMapping("/jobs/{jobId}/summary")
    @Operation(summary = "Get the Resume Analysis Dashboard KPI row for a job")
    public ResponseEntity<ApiResponse<ResumeMatchSummaryResponse>> getSummary(@PathVariable UUID jobId) {
        return ResponseEntity.ok(ApiResponse.ok("OK", resumeAnalysisQueryService.getSummary(jobId)));
    }

    @GetMapping("/top/{jobId}")
    @Operation(summary = "Top-ranked candidates for a job")
    public ResponseEntity<ApiResponse<List<ResumeMatchRankingItemResponse>>> getTop(@PathVariable UUID jobId) {
        return ResponseEntity.ok(ApiResponse.ok("OK", resumeAnalysisQueryService.getTop(jobId)));
    }

    @GetMapping("/recommendations")
    @Operation(summary = "Cross-job recommendations feed", description = "Optional job/department/minimum-score filters")
    public ResponseEntity<ApiResponse<List<ResumeMatchRankingItemResponse>>> getRecommendations(
            @RequestParam(required = false) UUID jobId,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) Integer minScore,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("OK", resumeAnalysisQueryService.getRecommendations(jobId, departmentId, minScore, pageable)));
    }

    @GetMapping("/applications/{applicationId}")
    @Operation(summary = "Get the full match analysis for an application (drawer main view)")
    public ResponseEntity<ApiResponse<ResumeMatchResponse>> getByApplication(@PathVariable UUID applicationId) {
        return ResponseEntity.ok(ApiResponse.ok("OK", resumeAnalysisQueryService.getByApplicationId(applicationId)));
    }

    @GetMapping("/candidates/{candidateId}")
    @Operation(summary = "Get a candidate's match analyses across every job they've applied to")
    public ResponseEntity<ApiResponse<List<CandidateAnalysisItemResponse>>> getByCandidate(@PathVariable UUID candidateId) {
        return ResponseEntity.ok(ApiResponse.ok("OK", resumeAnalysisQueryService.getByCandidateId(candidateId)));
    }

    @PostMapping("/recalculate/{applicationId}")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Recalculate an application's match analysis", description = "Also the retry path for a match stuck in AWAITING_RESUME/AWAITING_PARSE")
    public ResponseEntity<ApiResponse<Void>> recalculate(@PathVariable UUID applicationId) {
        resumeMatchAnalysisService.recalculate(applicationId);
        return ResponseEntity.accepted().body(ApiResponse.ok("Recalculation queued"));
    }

    @GetMapping("/{analysisId}/skills")
    @Operation(summary = "Get matched skills for an analysis")
    public ResponseEntity<ApiResponse<List<MatchedSkillResponse>>> getSkills(@PathVariable UUID analysisId) {
        return ResponseEntity.ok(ApiResponse.ok("OK", resumeAnalysisQueryService.getSkills(analysisId)));
    }

    @GetMapping("/{analysisId}/missing-skills")
    @Operation(summary = "Get missing skills for an analysis")
    public ResponseEntity<ApiResponse<List<MissingSkillResponse>>> getMissingSkills(@PathVariable UUID analysisId) {
        return ResponseEntity.ok(ApiResponse.ok("OK", resumeAnalysisQueryService.getMissingSkills(analysisId)));
    }

    @GetMapping("/{analysisId}/strengths")
    @Operation(summary = "Get AI-identified strengths for an analysis")
    public ResponseEntity<ApiResponse<List<StrengthWeaknessResponse>>> getStrengths(@PathVariable UUID analysisId) {
        return ResponseEntity.ok(ApiResponse.ok("OK", resumeAnalysisQueryService.getStrengths(analysisId)));
    }

    @GetMapping("/{analysisId}/weaknesses")
    @Operation(summary = "Get AI-identified weaknesses for an analysis")
    public ResponseEntity<ApiResponse<List<StrengthWeaknessResponse>>> getWeaknesses(@PathVariable UUID analysisId) {
        return ResponseEntity.ok(ApiResponse.ok("OK", resumeAnalysisQueryService.getWeaknesses(analysisId)));
    }

    @GetMapping("/{analysisId}/recommendation")
    @Operation(summary = "Get the AI hiring recommendation for an analysis")
    public ResponseEntity<ApiResponse<AiRecommendationResponse>> getRecommendation(@PathVariable UUID analysisId) {
        return ResponseEntity.ok(ApiResponse.ok("OK", resumeAnalysisQueryService.getRecommendation(analysisId)));
    }

    @GetMapping("/{analysisId}/summary")
    @Operation(summary = "Get the score/label breakdown for an analysis (drawer's compact summary)")
    public ResponseEntity<ApiResponse<ResumeMatchResponse>> getScoreSummary(@PathVariable UUID analysisId) {
        return ResponseEntity.ok(ApiResponse.ok("OK", resumeAnalysisQueryService.getScoreSummary(analysisId)));
    }
}
