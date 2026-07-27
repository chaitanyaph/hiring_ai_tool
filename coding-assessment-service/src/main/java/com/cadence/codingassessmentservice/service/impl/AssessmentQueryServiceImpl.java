package com.cadence.codingassessmentservice.service.impl;

import com.cadence.codingassessmentservice.constants.AssessmentStatus;
import com.cadence.codingassessmentservice.constants.CandidateAssessmentStatus;
import com.cadence.codingassessmentservice.dto.response.*;
import com.cadence.codingassessmentservice.entity.Assessment;
import com.cadence.codingassessmentservice.entity.CandidateAssessment;
import com.cadence.codingassessmentservice.exception.ErrorCode;
import com.cadence.codingassessmentservice.exception.ResourceNotFoundException;
import com.cadence.codingassessmentservice.feign.ApplicationServiceClient;
import com.cadence.codingassessmentservice.feign.CompanyServiceClient;
import com.cadence.codingassessmentservice.feign.JobServiceClient;
import com.cadence.codingassessmentservice.feign.dto.ApplicationSummaryDto;
import com.cadence.codingassessmentservice.feign.dto.CompanyDto;
import com.cadence.codingassessmentservice.feign.dto.JobDetailDto;
import com.cadence.codingassessmentservice.mapper.AssessmentMapper;
import com.cadence.codingassessmentservice.repository.AssessmentRepository;
import com.cadence.codingassessmentservice.repository.CandidateAssessmentRepository;
import com.cadence.codingassessmentservice.service.AssessmentQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssessmentQueryServiceImpl implements AssessmentQueryService {

    private final AssessmentRepository assessmentRepository;
    private final CandidateAssessmentRepository candidateAssessmentRepository;
    private final JobServiceClient jobServiceClient;
    private final ApplicationServiceClient applicationServiceClient;
    private final CompanyServiceClient companyServiceClient;
    private final AssessmentMapper assessmentMapper;

    @Override
    public PagedResponse<AssessmentListItemResponse> listAssessments(UUID companyId, AssessmentStatus status, Pageable pageable) {
        Page<Assessment> page = assessmentRepository.search(companyId, status, pageable);
        return PagedResponse.from(page.map(this::enrichListItem));
    }

    @Override
    public AssessmentResponse getAssessment(UUID companyId, UUID assessmentId) {
        Assessment assessment = findByIdAndCompanyOrThrow(companyId, assessmentId);
        return enrichAssessment(assessment);
    }

    @Override
    public AssessmentDetailsResponse getAssessmentDetails(UUID companyId, UUID assessmentId) {
        Assessment assessment = findByIdAndCompanyOrThrow(companyId, assessmentId);
        AssessmentResponse assessmentResponse = enrichAssessment(assessment);

        Map<UUID, ApplicationSummaryDto> applications = fetchApplicationsByJob(assessment.getJobId());
        List<InvitedCandidateResponse> invited = candidateAssessmentRepository.findAllByAssessmentId(assessmentId).stream()
                .map(ca -> {
                    ApplicationSummaryDto app = applications.get(ca.getApplicationId());
                    return InvitedCandidateResponse.builder()
                            .applicationId(ca.getApplicationId())
                            .candidateName(app != null ? app.getCandidateNameSnapshot() : null)
                            .candidateEmail(app != null ? app.getCandidateEmailSnapshot() : null)
                            .status(ca.getStatus())
                            .expiresAt(ca.getExpiresAt())
                            .build();
                })
                .toList();

        return AssessmentDetailsResponse.builder()
                .assessment(assessmentResponse)
                .rules(deriveRules(assessment))
                .invitedCandidates(invited)
                .build();
    }

    @Override
    public PagedResponse<CodingQueueItemResponse> getQueue(UUID companyId, UUID assessmentId, CandidateAssessmentStatus status, Pageable pageable) {
        Assessment assessment = findByIdAndCompanyOrThrow(companyId, assessmentId);
        Page<CandidateAssessment> page = candidateAssessmentRepository.search(assessmentId, status, pageable);
        Map<UUID, ApplicationSummaryDto> applications = fetchApplicationsByJob(assessment.getJobId());

        return PagedResponse.from(page.map(ca -> {
            ApplicationSummaryDto app = applications.get(ca.getApplicationId());
            return CodingQueueItemResponse.builder()
                    .applicationId(ca.getApplicationId())
                    .candidateName(app != null ? app.getCandidateNameSnapshot() : null)
                    .candidateEmail(app != null ? app.getCandidateEmailSnapshot() : null)
                    .jobTitle(app != null ? app.getJobTitleSnapshot() : null)
                    .status(ca.getStatus())
                    .dueOrCompletedAt(ca.getStatus() == CandidateAssessmentStatus.COMPLETED ? ca.getCompletedAt() : ca.getExpiresAt())
                    .build();
        }));
    }

    @Override
    public PagedResponse<CandidateAssessmentHistoryItemResponse> getCandidateHistory(UUID candidateId, Pageable pageable) {
        Page<CandidateAssessment> page = candidateAssessmentRepository.findAllByCandidateIdOrderByCreatedAtDesc(candidateId, pageable);
        return PagedResponse.from(page.map(ca -> {
            Assessment assessment = assessmentRepository.findById(ca.getAssessmentId()).orElse(null);
            String companyName = assessment != null ? companyName(assessment.getCompanyId()) : null;
            var relevantDate = switch (ca.getStatus()) {
                case COMPLETED -> ca.getCompletedAt();
                case EXPIRED -> ca.getExpiresAt();
                default -> ca.getExpiresAt();
            };
            return CandidateAssessmentHistoryItemResponse.builder()
                    .candidateAssessmentId(ca.getId())
                    .assessmentId(ca.getAssessmentId())
                    .assessmentName(assessment != null ? assessment.getName() : null)
                    .companyName(companyName)
                    .status(ca.getStatus())
                    .score(ca.getTotalScore())
                    .relevantDate(relevantDate)
                    .build();
        }));
    }

    @Override
    public CandidateAssessmentIntroResponse getCandidateIntro(UUID candidateAssessmentId) {
        CandidateAssessment ca = candidateAssessmentRepository.findById(candidateAssessmentId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CANDIDATE_ASSESSMENT_NOT_FOUND, "No assessment attempt found: " + candidateAssessmentId));
        Assessment assessment = assessmentRepository.findById(ca.getAssessmentId()).orElseThrow();

        JobDetailDto job = jobServiceClient.getJobDetail(assessment.getJobId()).getData();
        String companyName = companyName(assessment.getCompanyId());

        return CandidateAssessmentIntroResponse.builder()
                .candidateAssessmentId(ca.getId())
                .jobTitle(job != null ? job.getTitle() : null)
                .companyName(companyName)
                .durationMinutes(assessment.getDurationMinutes())
                .questionCount(assessment.getQuestionCount())
                .allowedLanguages(assessmentMapper.splitLanguages(assessment.getAllowedLanguages()))
                .status(ca.getStatus())
                .candidateInstructions(assessment.getCandidateInstructions())
                .build();
    }

    @Override
    public AssessmentResultResponse getCandidateResult(UUID candidateAssessmentId) {
        CandidateAssessment ca = candidateAssessmentRepository.findById(candidateAssessmentId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CANDIDATE_ASSESSMENT_NOT_FOUND, "No assessment attempt found: " + candidateAssessmentId));
        Assessment assessment = assessmentRepository.findById(ca.getAssessmentId()).orElseThrow();

        Integer scorePercent = ca.getTotalScore() == null || assessment.getTotalMarks() == 0 ? null
                : Math.round((float) ca.getTotalScore() * 100 / assessment.getTotalMarks());

        return AssessmentResultResponse.builder()
                .candidateAssessmentId(ca.getId())
                .scorePercent(scorePercent)
                .testCasesPassed(ca.getTestCasesPassed())
                .testCasesTotal(ca.getTestCasesTotal())
                .timeUsedMinutes(ca.getTimeUsedSeconds() == null ? null : ca.getTimeUsedSeconds() / 60)
                .autoSubmitted(ca.isAutoSubmitted())
                .submittedAt(ca.getCompletedAt())
                .build();
    }

    private List<String> deriveRules(Assessment assessment) {
        List<String> rules = new java.util.ArrayList<>();
        rules.add("Duration: " + assessment.getDurationMinutes() + " minutes");
        rules.add("Passing score: " + assessment.getPassingScorePercent() + "%");
        rules.add("Allowed languages: " + String.join(", ", assessmentMapper.splitLanguages(assessment.getAllowedLanguages())));
        if (assessment.isAntiCheatMonitoring()) {
            rules.add("Anti-cheat monitoring is enabled -- tab switches, fullscreen exits, and copy-paste are tracked");
        }
        if (assessment.isPlagiarismDetection()) {
            rules.add("Plagiarism detection is enabled");
        }
        if (assessment.isNegativeMarking()) {
            rules.add("Negative marking applies to incorrect answers");
        }
        return rules;
    }

    private AssessmentResponse enrichAssessment(Assessment assessment) {
        AssessmentResponse response = assessmentMapper.toResponse(assessment);
        JobDetailDto job = jobServiceClient.getJobDetail(assessment.getJobId()).getData();
        if (job != null) {
            response.setJobTitle(job.getTitle());
        }
        return response;
    }

    private AssessmentListItemResponse enrichListItem(Assessment assessment) {
        AssessmentListItemResponse response = assessmentMapper.toListItemResponse(assessment);
        JobDetailDto job = jobServiceClient.getJobDetail(assessment.getJobId()).getData();
        if (job != null) {
            response.setJobTitle(job.getTitle());
        }
        response.setInvitedCount(candidateAssessmentRepository.findAllByAssessmentId(assessment.getId()).size());
        return response;
    }

    private String companyName(UUID companyId) {
        CompanyDto company = companyServiceClient.getCompany(companyId).getData();
        return company != null ? company.getCompanyName() : null;
    }

    private Map<UUID, ApplicationSummaryDto> fetchApplicationsByJob(UUID jobId) {
        List<ApplicationSummaryDto> applications = applicationServiceClient.getApplicationsByJob(jobId).getData();
        if (applications == null) {
            return Map.of();
        }
        return applications.stream().collect(Collectors.toMap(ApplicationSummaryDto::getId, Function.identity(), (a, b) -> a));
    }

    private Assessment findByIdAndCompanyOrThrow(UUID companyId, UUID assessmentId) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ASSESSMENT_NOT_FOUND, "Assessment not found: " + assessmentId));
        if (!assessment.getCompanyId().equals(companyId)) {
            throw new ResourceNotFoundException(ErrorCode.ASSESSMENT_NOT_FOUND, "Assessment not found: " + assessmentId);
        }
        return assessment;
    }
}
