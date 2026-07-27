package com.cadence.candidateservice.service;

import com.cadence.candidateservice.client.CompanyServiceClient;
import com.cadence.candidateservice.client.JobServiceClient;
import com.cadence.candidateservice.client.dto.CompanyDto;
import com.cadence.candidateservice.client.dto.FeignApiResponse;
import com.cadence.candidateservice.client.dto.JobDto;
import com.cadence.candidateservice.constant.ApplicationStatus;
import com.cadence.candidateservice.dto.request.ApplyToJobRequest;
import com.cadence.candidateservice.dto.request.ChangeApplicationStageRequest;
import com.cadence.candidateservice.dto.response.ApplicationResponse;
import com.cadence.candidateservice.entity.Application;
import com.cadence.candidateservice.exception.CandidateValidationException;
import com.cadence.candidateservice.exception.DuplicateApplicationException;
import com.cadence.candidateservice.exception.InvalidStatusTransitionException;
import com.cadence.candidateservice.exception.ResourceNotFoundException;
import com.cadence.candidateservice.kafka.producer.CandidateEventProducer;
import com.cadence.candidateservice.mapper.ApplicationMapper;
import com.cadence.candidateservice.repository.ApplicationRepository;
import com.cadence.candidateservice.repository.ApplicationStatusHistoryRepository;
import com.cadence.candidateservice.security.CurrentUser;
import com.cadence.candidateservice.service.impl.ApplicationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceImplTest {

    @Mock private ApplicationRepository applicationRepository;
    @Mock private ApplicationStatusHistoryRepository historyRepository;
    @Mock private JobServiceClient jobServiceClient;
    @Mock private CompanyServiceClient companyServiceClient;
    @Mock private ApplicationMapper applicationMapper;
    @Mock private CandidateEventProducer eventProducer;

    @InjectMocks
    private ApplicationServiceImpl applicationService;

    private CurrentUser candidate;
    private CurrentUser recruiter;
    private UUID jobId;
    private UUID companyId;

    @BeforeEach
    void setUp() {
        jobId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        candidate = CurrentUser.builder().userId(UUID.randomUUID()).role("CANDIDATE").build();
        recruiter = CurrentUser.builder().userId(UUID.randomUUID()).role("HR_RECRUITER").companyId(companyId).build();
    }

    private JobDto publishedJob() {
        JobDto job = new JobDto();
        job.setId(jobId);
        job.setCompanyId(companyId);
        job.setTitle("Backend Engineer");
        job.setLocation("Pune, India");
        job.setEmploymentType("FULL_TIME");
        job.setStatus("PUBLISHED");
        return job;
    }

    @Test
    void apply_shouldThrow_whenAlreadyApplied() {
        when(applicationRepository.existsByCandidateIdAndJobId(candidate.getUserId(), jobId)).thenReturn(true);

        assertThatThrownBy(() -> applicationService.apply(candidate, ApplyToJobRequest.builder().jobId(jobId).build()))
                .isInstanceOf(DuplicateApplicationException.class);

        verify(jobServiceClient, never()).getJob(any());
    }

    @Test
    void apply_shouldThrow_whenJobNotPublished() {
        when(applicationRepository.existsByCandidateIdAndJobId(candidate.getUserId(), jobId)).thenReturn(false);
        JobDto draftJob = publishedJob();
        draftJob.setStatus("DRAFT");
        when(jobServiceClient.getJob(jobId)).thenReturn(new FeignApiResponse<>(true, "OK", draftJob));

        assertThatThrownBy(() -> applicationService.apply(candidate, ApplyToJobRequest.builder().jobId(jobId).build()))
                .isInstanceOf(CandidateValidationException.class);

        verify(applicationRepository, never()).save(any());
    }

    @Test
    void apply_shouldSucceed_whenJobPublished() {
        when(applicationRepository.existsByCandidateIdAndJobId(candidate.getUserId(), jobId)).thenReturn(false);
        when(jobServiceClient.getJob(jobId)).thenReturn(new FeignApiResponse<>(true, "OK", publishedJob()));
        when(companyServiceClient.getCompany(companyId)).thenReturn(new FeignApiResponse<>(true, "OK", new CompanyDto(companyId, "Acme Corp")));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> {
            Application a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });
        when(applicationMapper.toResponse(any(Application.class))).thenReturn(ApplicationResponse.builder().build());

        applicationService.apply(candidate, ApplyToJobRequest.builder().jobId(jobId).build());

        verify(historyRepository).save(argThat(h -> h.getFromStatus() == null && h.getToStatus() == ApplicationStatus.APPLIED));
        verify(eventProducer).publishApplicationSubmitted(any());
    }

    @Test
    void apply_shouldDegradeGracefully_whenCompanyServiceUnreachable() {
        when(applicationRepository.existsByCandidateIdAndJobId(candidate.getUserId(), jobId)).thenReturn(false);
        when(jobServiceClient.getJob(jobId)).thenReturn(new FeignApiResponse<>(true, "OK", publishedJob()));
        when(companyServiceClient.getCompany(companyId)).thenThrow(new RuntimeException("connection refused"));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));
        when(applicationMapper.toResponse(any(Application.class))).thenReturn(ApplicationResponse.builder().build());

        applicationService.apply(candidate, ApplyToJobRequest.builder().jobId(jobId).build());

        verify(eventProducer).publishApplicationSubmitted(any());
    }

    @Test
    void withdraw_shouldThrow_whenApplicationAlreadyTerminal() {
        Application app = Application.builder().id(UUID.randomUUID()).candidateId(candidate.getUserId())
                .jobId(jobId).companyId(companyId).status(ApplicationStatus.REJECTED).build();
        when(applicationRepository.findByIdAndCandidateId(app.getId(), candidate.getUserId())).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> applicationService.withdraw(candidate, app.getId()))
                .isInstanceOf(CandidateValidationException.class);
    }

    @Test
    void withdraw_shouldSucceed_whenWithdrawable() {
        Application app = Application.builder().id(UUID.randomUUID()).candidateId(candidate.getUserId())
                .jobId(jobId).companyId(companyId).status(ApplicationStatus.HR_INTERVIEW).build();
        when(applicationRepository.findByIdAndCandidateId(app.getId(), candidate.getUserId())).thenReturn(Optional.of(app));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));
        when(applicationMapper.toResponse(any(Application.class))).thenReturn(ApplicationResponse.builder().build());

        applicationService.withdraw(candidate, app.getId());

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.WITHDRAWN);
        assertThat(app.getWithdrawnAt()).isNotNull();
        verify(eventProducer).publishApplicationWithdrawn(any());
    }

    @Test
    void changeStage_shouldThrow_whenApplicationBelongsToDifferentCompany() {
        Application app = Application.builder().id(UUID.randomUUID()).candidateId(candidate.getUserId())
                .jobId(jobId).companyId(UUID.randomUUID()).status(ApplicationStatus.APPLIED).build();
        when(applicationRepository.findById(app.getId())).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> applicationService.changeStage(recruiter, app.getId(),
                ChangeApplicationStageRequest.builder().toStatus(ApplicationStatus.RESUME_SCREENING).build()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void changeStage_shouldThrow_whenTransitionIsInvalid() {
        Application app = Application.builder().id(UUID.randomUUID()).candidateId(candidate.getUserId())
                .jobId(jobId).companyId(companyId).status(ApplicationStatus.APPLIED).build();
        when(applicationRepository.findById(app.getId())).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> applicationService.changeStage(recruiter, app.getId(),
                ChangeApplicationStageRequest.builder().toStatus(ApplicationStatus.OFFER).build()))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void changeStage_shouldSucceed_whenTransitionIsValid() {
        Application app = Application.builder().id(UUID.randomUUID()).candidateId(candidate.getUserId())
                .jobId(jobId).companyId(companyId).status(ApplicationStatus.APPLIED).build();
        when(applicationRepository.findById(app.getId())).thenReturn(Optional.of(app));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));
        when(applicationMapper.toResponse(any(Application.class))).thenReturn(ApplicationResponse.builder().build());

        applicationService.changeStage(recruiter, app.getId(),
                ChangeApplicationStageRequest.builder().toStatus(ApplicationStatus.RESUME_SCREENING).note("Looks strong").build());

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.RESUME_SCREENING);
        verify(eventProducer).publishApplicationStageChanged(any());
    }
}
