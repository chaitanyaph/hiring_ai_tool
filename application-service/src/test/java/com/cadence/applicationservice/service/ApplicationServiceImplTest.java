package com.cadence.applicationservice.service;

import com.cadence.applicationservice.client.CandidateServiceClient;
import com.cadence.applicationservice.client.JobServiceClient;
import com.cadence.applicationservice.client.ResumeServiceClient;
import com.cadence.applicationservice.client.dto.CandidateDto;
import com.cadence.applicationservice.client.dto.FeignApiResponse;
import com.cadence.applicationservice.client.dto.JobDto;
import com.cadence.applicationservice.client.dto.ResumeDto;
import com.cadence.applicationservice.constant.ApplicationStatus;
import com.cadence.applicationservice.constant.HiringRecommendation;
import com.cadence.applicationservice.constant.InterviewType;
import com.cadence.applicationservice.constant.PlatformRole;
import com.cadence.applicationservice.dto.request.ApplyRequest;
import com.cadence.applicationservice.dto.request.StatusChangeRequest;
import com.cadence.applicationservice.dto.response.ApplicationResponse;
import com.cadence.applicationservice.entity.Application;
import com.cadence.applicationservice.exception.ApplicationValidationException;
import com.cadence.applicationservice.exception.DuplicateApplicationException;
import com.cadence.applicationservice.exception.InvalidStatusTransitionException;
import com.cadence.applicationservice.exception.ResourceNotFoundException;
import com.cadence.applicationservice.kafka.producer.ApplicationEventProducer;
import com.cadence.applicationservice.mapper.ApplicationMapper;
import com.cadence.applicationservice.repository.*;
import com.cadence.applicationservice.security.CurrentUser;
import com.cadence.applicationservice.service.impl.ApplicationServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceImplTest {

    @Mock private ApplicationRepository applicationRepository;
    @Mock private ApplicationStatusHistoryRepository statusHistoryRepository;
    @Mock private ApplicationStageHistoryRepository stageHistoryRepository;
    @Mock private ApplicationScoreRepository scoreRepository;
    @Mock private ApplicationNoteRepository noteRepository;
    @Mock private ApplicationEventRepository eventRepository;
    @Mock private JobServiceClient jobServiceClient;
    @Mock private CandidateServiceClient candidateServiceClient;
    @Mock private ResumeServiceClient resumeServiceClient;
    @Mock private ApplicationMapper applicationMapper;
    @Mock private ApplicationEventProducer eventProducer;
    @Mock private CacheManager cacheManager;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private ApplicationServiceImpl applicationService;

    private UUID jobId;
    private UUID companyId;
    private CurrentUser candidate;
    private CurrentUser recruiter;
    private CurrentUser hiringManagerNoPermission;
    private CurrentUser hiringManagerWithPermission;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(applicationService, "minProfileCompletionPercent", 60);
        jobId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        candidate = CurrentUser.builder().userId(UUID.randomUUID()).role(PlatformRole.CANDIDATE).build();
        recruiter = CurrentUser.builder().userId(UUID.randomUUID()).companyId(companyId).role(PlatformRole.HR_RECRUITER).permissions(Set.of()).build();
        hiringManagerNoPermission = CurrentUser.builder().userId(UUID.randomUUID()).companyId(companyId)
                .role(PlatformRole.HIRING_MANAGER).permissions(Set.of()).build();
        hiringManagerWithPermission = CurrentUser.builder().userId(UUID.randomUUID()).companyId(companyId)
                .role(PlatformRole.HIRING_MANAGER).permissions(Set.of(PlatformRole.APPLICATION_EDIT_PERMISSION)).build();
    }

    private JobDto publishedJob() {
        JobDto job = new JobDto();
        job.setId(jobId);
        job.setCompanyId(companyId);
        job.setTitle("Backend Engineer");
        job.setStatus("PUBLISHED");
        job.setApplicationDeadline(LocalDate.now().plusDays(10));
        return job;
    }

    private CandidateDto completeCandidate() {
        CandidateDto dto = new CandidateDto();
        dto.setId(candidate.getUserId());
        dto.setFullName("Rahul Mehta");
        dto.setEmail("rahul@email.com");
        dto.setResumeUploaded(true);
        dto.setProfileCompletionPercent(82);
        return dto;
    }

    @Test
    void apply_shouldThrow_whenAlreadyApplied() {
        when(applicationRepository.existsByCandidateIdAndJobId(candidate.getUserId(), jobId)).thenReturn(true);

        assertThatThrownBy(() -> applicationService.apply(candidate, ApplyRequest.builder().jobId(jobId).build()))
                .isInstanceOf(DuplicateApplicationException.class);

        verify(jobServiceClient, never()).getJob(any());
    }

    @Test
    void apply_shouldThrow_whenJobNotPublished() {
        when(applicationRepository.existsByCandidateIdAndJobId(candidate.getUserId(), jobId)).thenReturn(false);
        JobDto draft = publishedJob();
        draft.setStatus("DRAFT");
        when(jobServiceClient.getJob(jobId)).thenReturn(new FeignApiResponse<>(true, "OK", draft));

        assertThatThrownBy(() -> applicationService.apply(candidate, ApplyRequest.builder().jobId(jobId).build()))
                .isInstanceOf(ApplicationValidationException.class);

        verify(applicationRepository, never()).save(any());
    }

    @Test
    void apply_shouldThrow_whenDeadlinePassed() {
        when(applicationRepository.existsByCandidateIdAndJobId(candidate.getUserId(), jobId)).thenReturn(false);
        JobDto expired = publishedJob();
        expired.setApplicationDeadline(LocalDate.now().minusDays(1));
        when(jobServiceClient.getJob(jobId)).thenReturn(new FeignApiResponse<>(true, "OK", expired));

        assertThatThrownBy(() -> applicationService.apply(candidate, ApplyRequest.builder().jobId(jobId).build()))
                .isInstanceOf(ApplicationValidationException.class);
    }

    @Test
    void apply_shouldThrow_whenProfileIncomplete() {
        when(applicationRepository.existsByCandidateIdAndJobId(candidate.getUserId(), jobId)).thenReturn(false);
        when(jobServiceClient.getJob(jobId)).thenReturn(new FeignApiResponse<>(true, "OK", publishedJob()));
        CandidateDto lowCompletion = completeCandidate();
        lowCompletion.setProfileCompletionPercent(20);
        when(candidateServiceClient.getCandidateSummary(candidate.getUserId())).thenReturn(new FeignApiResponse<>(true, "OK", lowCompletion));

        assertThatThrownBy(() -> applicationService.apply(candidate, ApplyRequest.builder().jobId(jobId).build()))
                .isInstanceOf(ApplicationValidationException.class);
    }

    @Test
    void apply_shouldSucceed_andAutoAdvanceToResumeParsing() {
        UUID resumeId = UUID.randomUUID();
        ResumeDto resume = new ResumeDto();
        resume.setId(resumeId);
        resume.setCandidateId(candidate.getUserId());
        resume.setStatus("ACTIVE");

        when(applicationRepository.existsByCandidateIdAndJobId(candidate.getUserId(), jobId)).thenReturn(false);
        when(jobServiceClient.getJob(jobId)).thenReturn(new FeignApiResponse<>(true, "OK", publishedJob()));
        when(candidateServiceClient.getCandidateSummary(candidate.getUserId())).thenReturn(new FeignApiResponse<>(true, "OK", completeCandidate()));
        when(resumeServiceClient.getResume(resumeId)).thenReturn(new FeignApiResponse<>(true, "OK", resume));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> {
            Application a = inv.getArgument(0);
            if (a.getId() == null) a.setId(UUID.randomUUID());
            return a;
        });
        when(applicationMapper.toResponse(any(Application.class))).thenReturn(ApplicationResponse.builder().build());

        applicationService.apply(candidate, ApplyRequest.builder().jobId(jobId).resumeId(resumeId).build());

        var captor = org.mockito.ArgumentCaptor.forClass(Application.class);
        verify(applicationRepository, atLeastOnce()).save(captor.capture());
        Application saved = captor.getValue();
        assertThat(saved.getCurrentStatus()).isEqualTo(ApplicationStatus.RESUME_PARSING);
        assertThat(saved.getResumeId()).isEqualTo(resumeId);
        verify(eventProducer).publishApplicationCreated(any());
    }

    @Test
    void apply_shouldThrow_whenResumeBelongsToAnotherCandidate() {
        UUID resumeId = UUID.randomUUID();
        ResumeDto resume = new ResumeDto();
        resume.setId(resumeId);
        resume.setCandidateId(UUID.randomUUID());
        resume.setStatus("ACTIVE");

        when(applicationRepository.existsByCandidateIdAndJobId(candidate.getUserId(), jobId)).thenReturn(false);
        when(jobServiceClient.getJob(jobId)).thenReturn(new FeignApiResponse<>(true, "OK", publishedJob()));
        when(candidateServiceClient.getCandidateSummary(candidate.getUserId())).thenReturn(new FeignApiResponse<>(true, "OK", completeCandidate()));
        when(resumeServiceClient.getResume(resumeId)).thenReturn(new FeignApiResponse<>(true, "OK", resume));

        assertThatThrownBy(() -> applicationService.apply(candidate, ApplyRequest.builder().jobId(jobId).resumeId(resumeId).build()))
                .isInstanceOf(ApplicationValidationException.class);

        verify(applicationRepository, never()).save(any());
    }

    @Test
    void withdraw_shouldThrow_whenApplicationAlreadyTerminal() {
        Application app = Application.builder().id(UUID.randomUUID()).candidateId(candidate.getUserId())
                .companyId(companyId).jobId(jobId).currentStatus(ApplicationStatus.REJECTED).build();
        when(applicationRepository.findByIdAndCandidateId(app.getId(), candidate.getUserId())).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> applicationService.withdraw(candidate, app.getId()))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void withdraw_shouldSucceed_whenWithdrawable() {
        Application app = Application.builder().id(UUID.randomUUID()).candidateId(candidate.getUserId())
                .companyId(companyId).jobId(jobId).currentStatus(ApplicationStatus.HR_INTERVIEW).build();
        when(applicationRepository.findByIdAndCandidateId(app.getId(), candidate.getUserId())).thenReturn(Optional.of(app));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));
        when(applicationMapper.toResponse(any(Application.class))).thenReturn(ApplicationResponse.builder().build());

        applicationService.withdraw(candidate, app.getId());

        assertThat(app.getCurrentStatus()).isEqualTo(ApplicationStatus.WITHDRAWN);
        verify(eventProducer).publishApplicationWithdrawn(any());
    }

    @Test
    void acceptOffer_shouldAdvanceStraightToHired() {
        Application app = Application.builder().id(UUID.randomUUID()).candidateId(candidate.getUserId())
                .companyId(companyId).jobId(jobId).currentStatus(ApplicationStatus.OFFER_RELEASED).build();
        when(applicationRepository.findByIdAndCandidateId(app.getId(), candidate.getUserId())).thenReturn(Optional.of(app));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));
        when(applicationMapper.toResponse(any(Application.class))).thenReturn(ApplicationResponse.builder().build());

        applicationService.acceptOffer(candidate, app.getId());

        assertThat(app.getCurrentStatus()).isEqualTo(ApplicationStatus.HIRED);
        verify(eventProducer).publishOfferAccepted(any());
    }

    @Test
    void changeStatus_shouldDeny_hiringManagerWithoutPermission() {
        assertThatThrownBy(() -> applicationService.changeStatus(hiringManagerNoPermission, UUID.randomUUID(),
                StatusChangeRequest.builder().toStatus(ApplicationStatus.RESUME_PARSING).build()))
                .isInstanceOf(AccessDeniedException.class);

        verify(applicationRepository, never()).findByIdAndCompanyId(any(), any());
    }

    @Test
    void changeStatus_shouldAllow_hiringManagerWithPermission() {
        Application app = Application.builder().id(UUID.randomUUID()).candidateId(UUID.randomUUID())
                .companyId(companyId).jobId(jobId).currentStatus(ApplicationStatus.APPLIED).build();
        when(applicationRepository.findByIdAndCompanyId(app.getId(), companyId)).thenReturn(Optional.of(app));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));
        when(applicationMapper.toResponse(any(Application.class))).thenReturn(ApplicationResponse.builder().build());

        applicationService.changeStatus(hiringManagerWithPermission, app.getId(),
                StatusChangeRequest.builder().toStatus(ApplicationStatus.RESUME_PARSING).build());

        assertThat(app.getCurrentStatus()).isEqualTo(ApplicationStatus.RESUME_PARSING);
    }

    @Test
    void changeStatus_shouldThrow_onInvalidTransition() {
        Application app = Application.builder().id(UUID.randomUUID()).candidateId(UUID.randomUUID())
                .companyId(companyId).jobId(jobId).currentStatus(ApplicationStatus.APPLIED).build();
        when(applicationRepository.findByIdAndCompanyId(app.getId(), companyId)).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> applicationService.changeStatus(recruiter, app.getId(),
                StatusChangeRequest.builder().toStatus(ApplicationStatus.HIRED).build()))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void getForRecruiter_shouldThrow_whenApplicationBelongsToDifferentCompany() {
        when(applicationRepository.findByIdAndCompanyId(any(), eq(companyId))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.getForRecruiter(recruiter, UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void handleResumeParsed_shouldAdvanceThroughToAiMatching() {
        Application app = Application.builder().id(UUID.randomUUID()).candidateId(UUID.randomUUID())
                .companyId(companyId).jobId(jobId).currentStatus(ApplicationStatus.RESUME_PARSING).build();
        when(applicationRepository.findById(app.getId())).thenReturn(Optional.of(app));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        applicationService.handleResumeParsed(app.getId(), UUID.randomUUID());

        assertThat(app.getCurrentStatus()).isEqualTo(ApplicationStatus.AI_MATCHING);
    }

    @Test
    void handleResumeParsed_shouldNoOp_whenApplicationInUnexpectedStatus() {
        Application app = Application.builder().id(UUID.randomUUID()).candidateId(UUID.randomUUID())
                .companyId(companyId).jobId(jobId).currentStatus(ApplicationStatus.HIRED).build();
        when(applicationRepository.findById(app.getId())).thenReturn(Optional.of(app));

        applicationService.handleResumeParsed(app.getId(), UUID.randomUUID());

        assertThat(app.getCurrentStatus()).isEqualTo(ApplicationStatus.HIRED);
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void handleInterviewCompleted_ai_proceed_shouldAdvanceToCodingAssessmentPending() {
        Application app = Application.builder().id(UUID.randomUUID()).candidateId(UUID.randomUUID())
                .companyId(companyId).jobId(jobId).currentStatus(ApplicationStatus.AI_INTERVIEW_PENDING).build();
        when(applicationRepository.findById(app.getId())).thenReturn(Optional.of(app));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        applicationService.handleInterviewCompleted(app.getId(), InterviewType.AI, 85, "Strong candidate", HiringRecommendation.PROCEED);

        assertThat(app.getCurrentStatus()).isEqualTo(ApplicationStatus.CODING_ASSESSMENT_PENDING);
        assertThat(app.getAiInterviewScore()).isEqualTo(85);
    }

    @Test
    void handleInterviewCompleted_ai_reject_shouldAutoReject() {
        Application app = Application.builder().id(UUID.randomUUID()).candidateId(UUID.randomUUID())
                .companyId(companyId).jobId(jobId).currentStatus(ApplicationStatus.AI_INTERVIEW_PENDING).build();
        when(applicationRepository.findById(app.getId())).thenReturn(Optional.of(app));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        applicationService.handleInterviewCompleted(app.getId(), InterviewType.AI, 22, "Weak candidate", HiringRecommendation.REJECT);

        assertThat(app.getCurrentStatus()).isEqualTo(ApplicationStatus.REJECTED);
        assertThat(app.getAiInterviewScore()).isEqualTo(22);
    }

    @Test
    void handleInterviewCompleted_ai_hold_shouldStopAtCompletedForManualReview() {
        Application app = Application.builder().id(UUID.randomUUID()).candidateId(UUID.randomUUID())
                .companyId(companyId).jobId(jobId).currentStatus(ApplicationStatus.AI_INTERVIEW_PENDING).build();
        when(applicationRepository.findById(app.getId())).thenReturn(Optional.of(app));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        applicationService.handleInterviewCompleted(app.getId(), InterviewType.AI, 55, "Mixed signals", HiringRecommendation.HOLD);

        assertThat(app.getCurrentStatus()).isEqualTo(ApplicationStatus.AI_INTERVIEW_COMPLETED);
        assertThat(app.getAiInterviewScore()).isEqualTo(55);
    }

    @Test
    void handleBackgroundVerificationCompleted_shouldReject_whenFailed() {
        Application app = Application.builder().id(UUID.randomUUID()).candidateId(UUID.randomUUID())
                .companyId(companyId).jobId(jobId).currentStatus(ApplicationStatus.BACKGROUND_VERIFICATION).build();
        when(applicationRepository.findById(app.getId())).thenReturn(Optional.of(app));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        applicationService.handleBackgroundVerificationCompleted(app.getId(), false, "Discrepancy found");

        assertThat(app.getCurrentStatus()).isEqualTo(ApplicationStatus.REJECTED);
    }
}
