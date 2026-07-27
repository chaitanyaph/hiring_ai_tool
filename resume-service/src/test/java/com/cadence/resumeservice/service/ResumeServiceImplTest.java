package com.cadence.resumeservice.service;

import com.cadence.resumeservice.constants.PlatformRole;
import com.cadence.resumeservice.constants.ResumeStatus;
import com.cadence.resumeservice.dto.request.RenameResumeRequest;
import com.cadence.resumeservice.dto.response.ResumeResponse;
import com.cadence.resumeservice.entity.Resume;
import com.cadence.resumeservice.exception.ResourceNotFoundException;
import com.cadence.resumeservice.exception.ResumeConflictException;
import com.cadence.resumeservice.exception.ResumeValidationException;
import com.cadence.resumeservice.feign.ApplicationServiceClient;
import com.cadence.resumeservice.feign.CandidateServiceClient;
import com.cadence.resumeservice.feign.dto.CandidateDto;
import com.cadence.resumeservice.feign.dto.FeignApiResponse;
import com.cadence.resumeservice.kafka.producer.ResumeEventProducer;
import com.cadence.resumeservice.mapper.ResumeMapper;
import com.cadence.resumeservice.minio.MinioStorageService;
import com.cadence.resumeservice.repository.ResumeRepository;
import com.cadence.resumeservice.security.CurrentUser;
import com.cadence.resumeservice.service.impl.ResumeServiceImpl;
import com.cadence.resumeservice.validation.FileValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeServiceImplTest {

    @Mock private ResumeRepository resumeRepository;
    @Mock private FileValidator fileValidator;
    @Mock private MinioStorageService minioStorageService;
    @Mock private CandidateServiceClient candidateServiceClient;
    @Mock private ApplicationServiceClient applicationServiceClient;
    @Mock private ResumeMapper resumeMapper;
    @Mock private ResumeEventProducer eventProducer;
    @Mock private CacheManager cacheManager;

    @InjectMocks
    private ResumeServiceImpl resumeService;

    private CurrentUser candidate;
    private CurrentUser recruiter;
    private CurrentUser admin;
    private UUID companyId;
    private MockMultipartFile pdfFile;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(resumeService, "maxResumesPerCandidate", 3);
        ReflectionTestUtils.setField(resumeService, "bucket", "candidate-resumes");

        companyId = UUID.randomUUID();
        candidate = CurrentUser.builder().userId(UUID.randomUUID()).role(PlatformRole.CANDIDATE).build();
        recruiter = CurrentUser.builder().userId(UUID.randomUUID()).companyId(companyId).role(PlatformRole.HR_RECRUITER).build();
        admin = CurrentUser.builder().userId(UUID.randomUUID()).role(PlatformRole.ADMIN).build();

        pdfFile = new MockMultipartFile("file", "resume.pdf", "application/pdf", "%PDF-1.4 fake content".getBytes());
    }

    private CandidateDto activeCandidate() {
        CandidateDto dto = new CandidateDto();
        dto.setId(candidate.getUserId());
        dto.setFullName("Rahul Mehta");
        dto.setStatus("ACTIVE");
        return dto;
    }

    @Test
    void upload_shouldThrow_whenCandidateNotActive() {
        doNothing().when(fileValidator).validate(any());
        CandidateDto inactive = activeCandidate();
        inactive.setStatus("SUSPENDED");
        when(candidateServiceClient.getCandidateSummary(candidate.getUserId())).thenReturn(new FeignApiResponse<>(true, "OK", inactive));

        assertThatThrownBy(() -> resumeService.upload(candidate, pdfFile, null))
                .isInstanceOf(ResumeValidationException.class);

        verify(resumeRepository, never()).save(any());
    }

    @Test
    void upload_shouldThrow_whenResumeLimitExceeded() {
        doNothing().when(fileValidator).validate(any());
        when(candidateServiceClient.getCandidateSummary(candidate.getUserId())).thenReturn(new FeignApiResponse<>(true, "OK", activeCandidate()));
        when(resumeRepository.countByCandidateIdAndStatus(candidate.getUserId(), ResumeStatus.ACTIVE)).thenReturn(3L);

        assertThatThrownBy(() -> resumeService.upload(candidate, pdfFile, null))
                .isInstanceOf(ResumeConflictException.class);

        verify(minioStorageService, never()).upload(any(), any(), any(), any());
    }

    @Test
    void upload_shouldThrow_whenDuplicateChecksum() {
        doNothing().when(fileValidator).validate(any());
        when(candidateServiceClient.getCandidateSummary(candidate.getUserId())).thenReturn(new FeignApiResponse<>(true, "OK", activeCandidate()));
        when(resumeRepository.countByCandidateIdAndStatus(candidate.getUserId(), ResumeStatus.ACTIVE)).thenReturn(1L);
        when(resumeRepository.existsByCandidateIdAndChecksumAndStatus(eq(candidate.getUserId()), any(), eq(ResumeStatus.ACTIVE))).thenReturn(true);

        assertThatThrownBy(() -> resumeService.upload(candidate, pdfFile, null))
                .isInstanceOf(ResumeConflictException.class);

        verify(minioStorageService, never()).upload(any(), any(), any(), any());
    }

    @Test
    void upload_shouldMarkAsDefault_whenFirstResume() {
        doNothing().when(fileValidator).validate(any());
        when(candidateServiceClient.getCandidateSummary(candidate.getUserId())).thenReturn(new FeignApiResponse<>(true, "OK", activeCandidate()));
        when(resumeRepository.countByCandidateIdAndStatus(candidate.getUserId(), ResumeStatus.ACTIVE)).thenReturn(0L);
        when(resumeRepository.existsByCandidateIdAndChecksumAndStatus(any(), any(), any())).thenReturn(false);
        when(minioStorageService.buildObjectName(any(), any())).thenReturn(candidate.getUserId() + "/uuid.pdf");
        when(resumeRepository.save(any(Resume.class))).thenAnswer(inv -> inv.getArgument(0));
        when(resumeMapper.toResponse(any(Resume.class))).thenReturn(ResumeResponse.builder().build());

        resumeService.upload(candidate, pdfFile, "My Resume");

        var captor = org.mockito.ArgumentCaptor.forClass(Resume.class);
        verify(resumeRepository).save(captor.capture());
        assertThat(captor.getValue().isDefaultResume()).isTrue();
        assertThat(captor.getValue().getDisplayName()).isEqualTo("My Resume");
        verify(eventProducer).publishResumeUploaded(any());
        verify(eventProducer).publishResumeDefaultChanged(any());
    }

    @Test
    void upload_shouldNotMarkAsDefault_whenNotFirstResume() {
        doNothing().when(fileValidator).validate(any());
        when(candidateServiceClient.getCandidateSummary(candidate.getUserId())).thenReturn(new FeignApiResponse<>(true, "OK", activeCandidate()));
        when(resumeRepository.countByCandidateIdAndStatus(candidate.getUserId(), ResumeStatus.ACTIVE)).thenReturn(1L);
        when(resumeRepository.existsByCandidateIdAndChecksumAndStatus(any(), any(), any())).thenReturn(false);
        when(minioStorageService.buildObjectName(any(), any())).thenReturn(candidate.getUserId() + "/uuid2.pdf");
        when(resumeRepository.save(any(Resume.class))).thenAnswer(inv -> inv.getArgument(0));
        when(resumeMapper.toResponse(any(Resume.class))).thenReturn(ResumeResponse.builder().build());

        resumeService.upload(candidate, pdfFile, null);

        var captor = org.mockito.ArgumentCaptor.forClass(Resume.class);
        verify(resumeRepository).save(captor.capture());
        assertThat(captor.getValue().isDefaultResume()).isFalse();
        verify(eventProducer, never()).publishResumeDefaultChanged(any());
    }

    @Test
    void setDefault_shouldUnsetPreviousDefault() {
        Resume newDefault = Resume.builder().id(UUID.randomUUID()).candidateId(candidate.getUserId())
                .status(ResumeStatus.ACTIVE).defaultResume(false).build();
        Resume oldDefault = Resume.builder().id(UUID.randomUUID()).candidateId(candidate.getUserId())
                .status(ResumeStatus.ACTIVE).defaultResume(true).build();

        when(resumeRepository.findByIdAndCandidateId(newDefault.getId(), candidate.getUserId())).thenReturn(Optional.of(newDefault));
        when(resumeRepository.findByCandidateIdAndDefaultResumeTrueAndStatus(candidate.getUserId(), ResumeStatus.ACTIVE)).thenReturn(Optional.of(oldDefault));
        when(resumeRepository.save(any(Resume.class))).thenAnswer(inv -> inv.getArgument(0));
        when(resumeMapper.toResponse(any(Resume.class))).thenReturn(ResumeResponse.builder().build());

        resumeService.setDefault(candidate, newDefault.getId());

        assertThat(oldDefault.isDefaultResume()).isFalse();
        assertThat(newDefault.isDefaultResume()).isTrue();
        verify(eventProducer).publishResumeDefaultChanged(any());
    }

    @Test
    void delete_shouldThrow_whenResumeInUse() {
        Resume resume = Resume.builder().id(UUID.randomUUID()).candidateId(candidate.getUserId()).status(ResumeStatus.ACTIVE).build();
        when(resumeRepository.findByIdAndCandidateId(resume.getId(), candidate.getUserId())).thenReturn(Optional.of(resume));
        when(applicationServiceClient.isResumeInUse(resume.getId())).thenReturn(new FeignApiResponse<>(true, "OK", true));

        assertThatThrownBy(() -> resumeService.delete(candidate, resume.getId()))
                .isInstanceOf(ResumeConflictException.class);

        verify(resumeRepository, never()).save(any());
    }

    @Test
    void delete_shouldPromoteNextDefault_whenDefaultResumeDeleted() {
        Resume toDelete = Resume.builder().id(UUID.randomUUID()).candidateId(candidate.getUserId())
                .status(ResumeStatus.ACTIVE).defaultResume(true).build();
        Resume remaining = Resume.builder().id(UUID.randomUUID()).candidateId(candidate.getUserId())
                .status(ResumeStatus.ACTIVE).defaultResume(false).build();

        when(resumeRepository.findByIdAndCandidateId(toDelete.getId(), candidate.getUserId())).thenReturn(Optional.of(toDelete));
        when(applicationServiceClient.isResumeInUse(toDelete.getId())).thenReturn(new FeignApiResponse<>(true, "OK", false));
        when(resumeRepository.save(any(Resume.class))).thenAnswer(inv -> inv.getArgument(0));
        when(resumeRepository.findAllByCandidateIdAndStatusOrderByUploadedAtDesc(candidate.getUserId(), ResumeStatus.ACTIVE))
                .thenReturn(List.of(remaining));

        resumeService.delete(candidate, toDelete.getId());

        assertThat(toDelete.getStatus()).isEqualTo(ResumeStatus.DELETED);
        assertThat(remaining.isDefaultResume()).isTrue();
        verify(eventProducer).publishResumeDeleted(any());
        verify(eventProducer).publishResumeDefaultChanged(any());
    }

    @Test
    void downloadForRecruiter_shouldThrow_whenCandidateNeverAppliedToCompany() {
        Resume resume = Resume.builder().id(UUID.randomUUID()).candidateId(UUID.randomUUID()).status(ResumeStatus.ACTIVE).build();
        when(resumeRepository.findById(resume.getId())).thenReturn(Optional.of(resume));
        when(applicationServiceClient.hasApplicationFromCandidateToCompany(resume.getCandidateId(), companyId))
                .thenReturn(new FeignApiResponse<>(true, "OK", false));

        assertThatThrownBy(() -> resumeService.downloadForRecruiter(recruiter, resume.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void downloadForRecruiter_shouldBypassCompanyCheck_whenAdmin() {
        Resume resume = Resume.builder().id(UUID.randomUUID()).candidateId(UUID.randomUUID()).status(ResumeStatus.ACTIVE)
                .bucketName("candidate-resumes").objectName("obj.pdf").originalFileName("resume.pdf")
                .mimeType("application/pdf").fileSize(100L).build();
        when(resumeRepository.findById(resume.getId())).thenReturn(Optional.of(resume));
        when(minioStorageService.download(any(), any())).thenReturn(new java.io.ByteArrayInputStream(new byte[0]));

        resumeService.downloadForRecruiter(admin, resume.getId());

        verify(applicationServiceClient, never()).hasApplicationFromCandidateToCompany(any(), any());
    }

    @Test
    void getDetail_shouldThrow_whenResumeBelongsToDifferentCandidate() {
        when(resumeRepository.findByIdAndCandidateId(any(), eq(candidate.getUserId()))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resumeService.getDetail(candidate, UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void rename_shouldUpdateDisplayName() {
        Resume resume = Resume.builder().id(UUID.randomUUID()).candidateId(candidate.getUserId())
                .status(ResumeStatus.ACTIVE).displayName("Old name").build();
        when(resumeRepository.findByIdAndCandidateId(resume.getId(), candidate.getUserId())).thenReturn(Optional.of(resume));
        when(resumeRepository.save(any(Resume.class))).thenAnswer(inv -> inv.getArgument(0));
        when(resumeMapper.toResponse(any(Resume.class))).thenReturn(ResumeResponse.builder().build());

        resumeService.rename(candidate, resume.getId(), RenameResumeRequest.builder().displayName("New name").build());

        assertThat(resume.getDisplayName()).isEqualTo("New name");
    }
}
