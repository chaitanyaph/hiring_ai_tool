package com.cadence.jobservice.service;

import com.cadence.jobservice.client.CompanyServiceClient;
import com.cadence.jobservice.constant.JobStatus;
import com.cadence.jobservice.constant.PlatformRole;
import com.cadence.jobservice.dto.request.JobBasicInfoRequest;
import com.cadence.jobservice.dto.request.StatusChangeRequest;
import com.cadence.jobservice.entity.Job;
import com.cadence.jobservice.exception.InvalidStatusTransitionException;
import com.cadence.jobservice.exception.JobValidationException;
import com.cadence.jobservice.exception.ResourceNotFoundException;
import com.cadence.jobservice.kafka.producer.JobEventProducer;
import com.cadence.jobservice.mapper.JobMapper;
import com.cadence.jobservice.mapper.PipelineStageMapper;
import com.cadence.jobservice.mapper.SkillMapper;
import com.cadence.jobservice.repository.*;
import com.cadence.jobservice.security.CurrentUser;
import com.cadence.jobservice.service.impl.JobServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceImplTest {

    @Mock private JobRepository jobRepository;
    @Mock private JobDescriptionRepository jobDescriptionRepository;
    @Mock private JobRequirementsRepository jobRequirementsRepository;
    @Mock private JobSkillRepository jobSkillRepository;
    @Mock private JobBenefitRepository jobBenefitRepository;
    @Mock private JobPipelineStageRepository jobPipelineStageRepository;
    @Mock private JobAssignmentRepository jobAssignmentRepository;
    @Mock private JobStatusHistoryRepository jobStatusHistoryRepository;
    @Mock private JobAuditRepository jobAuditRepository;
    @Mock private JobMapper jobMapper;
    @Mock private SkillMapper skillMapper;
    @Mock private PipelineStageMapper pipelineStageMapper;
    @Mock private JobEventProducer eventProducer;
    @Mock private CompanyServiceClient companyServiceClient;

    @InjectMocks
    private JobServiceImpl jobService;

    private UUID companyId;
    private CurrentUser recruiter;
    private CurrentUser hiringManagerNoPermission;
    private CurrentUser hiringManagerWithPermission;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        recruiter = CurrentUser.builder().userId(UUID.randomUUID()).companyId(companyId)
                .role(PlatformRole.HR_RECRUITER).permissions(Set.of()).build();
        hiringManagerNoPermission = CurrentUser.builder().userId(UUID.randomUUID()).companyId(companyId)
                .role(PlatformRole.HIRING_MANAGER).permissions(Set.of()).build();
        hiringManagerWithPermission = CurrentUser.builder().userId(UUID.randomUUID()).companyId(companyId)
                .role(PlatformRole.HIRING_MANAGER).permissions(Set.of(PlatformRole.JOB_EDIT_PERMISSION)).build();
    }

    @Test
    void createDraft_shouldSeedElevenDefaultPipelineStages() {
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> {
            Job j = inv.getArgument(0);
            j.setId(UUID.randomUUID());
            return j;
        });
        when(jobMapper.toDetail(any(Job.class))).thenReturn(com.cadence.jobservice.dto.response.JobDetailResponse.builder().build());

        JobBasicInfoRequest request = JobBasicInfoRequest.builder().title("Backend Engineer").build();
        jobService.createDraft(request, recruiter);

        ArgumentCaptor<com.cadence.jobservice.entity.JobPipelineStage> captor =
                ArgumentCaptor.forClass(com.cadence.jobservice.entity.JobPipelineStage.class);
        verify(jobPipelineStageRepository, times(11)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getStageName()).isEqualTo("Application Received");
        assertThat(captor.getAllValues().get(10).getStageName()).isEqualTo("Hired");
        verify(eventProducer).publishJobCreated(any());
    }

    @Test
    void publishJob_shouldThrow_whenNoRecruiterAssigned() {
        UUID jobId = UUID.randomUUID();
        Job job = Job.builder().id(jobId).companyId(companyId).status(JobStatus.DRAFT)
                .departmentId(UUID.randomUUID()).numberOfOpenings(2).build();
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobPipelineStageRepository.findAllByJobIdOrderByStageOrderAsc(jobId))
                .thenReturn(java.util.List.of(com.cadence.jobservice.entity.JobPipelineStage.builder().enabled(true).build()));

        assertThatThrownBy(() -> jobService.publishJob(jobId, recruiter))
                .isInstanceOf(JobValidationException.class)
                .hasMessageContaining("recruiter assignment");

        verify(jobRepository, never()).save(argThat(j -> j.getStatus() == JobStatus.PUBLISHED));
    }

    @Test
    void publishJob_shouldThrow_whenDeadlineInPast() {
        UUID jobId = UUID.randomUUID();
        Job job = Job.builder().id(jobId).companyId(companyId).status(JobStatus.DRAFT)
                .departmentId(UUID.randomUUID()).numberOfOpenings(2)
                .recruiterId(UUID.randomUUID()).applicationDeadline(LocalDate.now().minusDays(1)).build();
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> jobService.publishJob(jobId, recruiter))
                .isInstanceOf(JobValidationException.class)
                .hasMessageContaining("past");
    }

    @Test
    void publishJob_shouldSucceed_whenAllRequirementsMet() {
        UUID jobId = UUID.randomUUID();
        Job job = Job.builder().id(jobId).companyId(companyId).status(JobStatus.DRAFT)
                .departmentId(UUID.randomUUID()).numberOfOpenings(2).recruiterId(UUID.randomUUID())
                .title("Backend Engineer").jobCode("JOB-2026-00001").build();
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobPipelineStageRepository.findAllByJobIdOrderByStageOrderAsc(jobId))
                .thenReturn(java.util.List.of(com.cadence.jobservice.entity.JobPipelineStage.builder().enabled(true).build()));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jobMapper.toDetail(any(Job.class))).thenReturn(com.cadence.jobservice.dto.response.JobDetailResponse.builder().build());

        jobService.publishJob(jobId, recruiter);

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(JobStatus.PUBLISHED);
        assertThat(captor.getValue().getPublishedAt()).isNotNull();
        verify(eventProducer).publishJobPublished(any());
    }

    @Test
    void closeJob_shouldThrow_whenJobIsStillDraft() {
        UUID jobId = UUID.randomUUID();
        Job job = Job.builder().id(jobId).companyId(companyId).status(JobStatus.DRAFT).build();
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> jobService.closeJob(jobId, new StatusChangeRequest(), recruiter))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void getJobDetail_shouldThrow_whenJobBelongsToDifferentCompany() {
        UUID jobId = UUID.randomUUID();
        Job job = Job.builder().id(jobId).companyId(UUID.randomUUID()).status(JobStatus.PUBLISHED).build();
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> jobService.getJobDetail(jobId, recruiter))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateBasicInfo_shouldDeny_hiringManagerWithoutEditPermission() {
        UUID jobId = UUID.randomUUID();
        Job job = Job.builder().id(jobId).companyId(companyId).status(JobStatus.DRAFT).build();
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        JobBasicInfoRequest request = JobBasicInfoRequest.builder().title("New Title").build();

        assertThatThrownBy(() -> jobService.updateBasicInfo(jobId, request, hiringManagerNoPermission))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateBasicInfo_shouldAllow_hiringManagerWithEditPermission() {
        UUID jobId = UUID.randomUUID();
        Job job = Job.builder().id(jobId).companyId(companyId).status(JobStatus.DRAFT).build();
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jobMapper.toDetail(any(Job.class))).thenReturn(com.cadence.jobservice.dto.response.JobDetailResponse.builder().build());

        JobBasicInfoRequest request = JobBasicInfoRequest.builder().title("New Title").build();
        jobService.updateBasicInfo(jobId, request, hiringManagerWithPermission);

        verify(jobRepository).save(any(Job.class));
    }

    @Test
    void deleteJob_shouldThrow_whenJobIsNotDraft() {
        UUID jobId = UUID.randomUUID();
        Job job = Job.builder().id(jobId).companyId(companyId).status(JobStatus.PUBLISHED).build();
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> jobService.deleteJob(jobId, recruiter))
                .isInstanceOf(JobValidationException.class);

        verify(jobRepository, never()).save(any());
    }

    @Test
    void deleteJob_shouldSoftDelete_whenJobIsDraft() {
        UUID jobId = UUID.randomUUID();
        Job job = Job.builder().id(jobId).companyId(companyId).status(JobStatus.DRAFT).build();
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        jobService.deleteJob(jobId, recruiter);

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(captor.capture());
        assertThat(captor.getValue().isDeleted()).isTrue();
        verify(jobRepository, never()).delete(any(Job.class));
    }
}
