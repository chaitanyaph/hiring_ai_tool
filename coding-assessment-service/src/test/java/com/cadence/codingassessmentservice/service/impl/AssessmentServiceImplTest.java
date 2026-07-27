package com.cadence.codingassessmentservice.service.impl;

import com.cadence.codingassessmentservice.constants.AssessmentStatus;
import com.cadence.codingassessmentservice.constants.AssessmentType;
import com.cadence.codingassessmentservice.constants.Difficulty;
import com.cadence.codingassessmentservice.constants.ProgrammingLanguage;
import com.cadence.codingassessmentservice.dto.request.CreateAssessmentRequest;
import com.cadence.codingassessmentservice.dto.request.UpdateAssessmentRequest;
import com.cadence.codingassessmentservice.entity.Assessment;
import com.cadence.codingassessmentservice.exception.AssessmentConflictException;
import com.cadence.codingassessmentservice.exception.ResourceNotFoundException;
import com.cadence.codingassessmentservice.feign.JobServiceClient;
import com.cadence.codingassessmentservice.feign.dto.FeignApiResponse;
import com.cadence.codingassessmentservice.feign.dto.JobDetailDto;
import com.cadence.codingassessmentservice.kafka.producer.CodingAssessmentEventProducer;
import com.cadence.codingassessmentservice.mapper.AssessmentMapper;
import com.cadence.codingassessmentservice.mapper.AssessmentMapperImpl;
import com.cadence.codingassessmentservice.repository.AssessmentQuestionRepository;
import com.cadence.codingassessmentservice.repository.AssessmentRepository;
import com.cadence.codingassessmentservice.repository.CandidateAssessmentRepository;
import com.cadence.codingassessmentservice.service.AssessmentEligibilityService;
import com.cadence.codingassessmentservice.service.CandidateAssessmentService;
import com.cadence.codingassessmentservice.service.EligibleCandidate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssessmentServiceImplTest {

    @Mock private AssessmentRepository assessmentRepository;
    @Mock private AssessmentQuestionRepository assessmentQuestionRepository;
    @Mock private CandidateAssessmentRepository candidateAssessmentRepository;
    @Mock private JobServiceClient jobServiceClient;
    @Mock private AssessmentEligibilityService assessmentEligibilityService;
    @Mock private CandidateAssessmentService candidateAssessmentService;
    @Mock private CodingAssessmentEventProducer eventProducer;

    private final AssessmentMapper assessmentMapper = new AssessmentMapperImpl();

    private AssessmentServiceImpl assessmentService;

    private UUID companyId;
    private UUID jobId;
    private UUID recruiterId;

    private final java.util.concurrent.atomic.AtomicReference<Assessment> savedAssessment = new java.util.concurrent.atomic.AtomicReference<>();

    @BeforeEach
    void setUp() {
        assessmentService = new AssessmentServiceImpl(assessmentRepository, assessmentQuestionRepository,
                candidateAssessmentRepository, jobServiceClient, assessmentEligibilityService,
                candidateAssessmentService, eventProducer, assessmentMapper);
        companyId = UUID.randomUUID();
        jobId = UUID.randomUUID();
        recruiterId = UUID.randomUUID();
        // save() mutates and returns the same instance; findById() always reflects
        // whatever was most recently saved -- mirrors a real in-place JPA update.
        lenient().when(assessmentRepository.save(any(Assessment.class))).thenAnswer(inv -> {
            Assessment a = inv.getArgument(0);
            savedAssessment.set(a);
            return a;
        });
        lenient().when(assessmentRepository.findById(any())).thenAnswer(inv -> Optional.ofNullable(savedAssessment.get()));
        lenient().when(jobServiceClient.getJobDetail(any())).thenReturn(new FeignApiResponse<>(true, "OK", jobDetail()));
    }

    @Test
    void createAssessment_shouldPersistAsDraft_whenPublishNowIsFalse() {
        CreateAssessmentRequest request = baseRequest().publishNow(false).build();

        var response = assessmentService.createAssessment(companyId, recruiterId, request);

        assertThat(response.getStatus()).isEqualTo(AssessmentStatus.DRAFT);
        verify(assessmentEligibilityService, never()).findUninvitedEligibleCandidates(any(), any());
    }

    @Test
    void createAssessment_shouldPublishAndInviteEligibleCandidates_whenPublishNowIsTrue() {
        CreateAssessmentRequest request = baseRequest().publishNow(true).build();
        UUID applicationId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        when(assessmentEligibilityService.findUninvitedEligibleCandidates(eq(jobId), any()))
                .thenReturn(List.of(new EligibleCandidate(applicationId, candidateId)));

        var response = assessmentService.createAssessment(companyId, recruiterId, request);

        assertThat(response.getStatus()).isEqualTo(AssessmentStatus.PUBLISHED);
        verify(candidateAssessmentService).inviteCandidate(any(), eq(applicationId), eq(jobId), eq(candidateId));
    }

    @Test
    void updateAssessment_shouldThrow_whenAssessmentIsNotDraft() {
        UUID assessmentId = UUID.randomUUID();
        Assessment published = Assessment.builder().id(assessmentId).companyId(companyId).jobId(jobId)
                .status(AssessmentStatus.PUBLISHED).allowedLanguages("JAVA").build();
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(published));

        UpdateAssessmentRequest request = UpdateAssessmentRequest.builder()
                .name("x").type(AssessmentType.CODING).allowedLanguages(List.of(ProgrammingLanguage.JAVA))
                .difficulty(Difficulty.EASY).durationMinutes(10).questionCount(1).passingScorePercent(50).totalMarks(100).build();

        assertThatThrownBy(() -> assessmentService.updateAssessment(companyId, assessmentId, request))
                .isInstanceOf(AssessmentConflictException.class);
    }

    @Test
    void deleteAssessment_shouldThrow_whenAssessmentIsNotDraft() {
        UUID assessmentId = UUID.randomUUID();
        Assessment published = Assessment.builder().id(assessmentId).companyId(companyId).jobId(jobId)
                .status(AssessmentStatus.PUBLISHED).allowedLanguages("JAVA").build();
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(published));

        assertThatThrownBy(() -> assessmentService.deleteAssessment(companyId, assessmentId))
                .isInstanceOf(AssessmentConflictException.class);
        verify(assessmentRepository, never()).delete(any());
    }

    @Test
    void publishAssessment_shouldThrow_whenAlreadyPublished() {
        UUID assessmentId = UUID.randomUUID();
        Assessment published = Assessment.builder().id(assessmentId).companyId(companyId).jobId(jobId)
                .status(AssessmentStatus.PUBLISHED).allowedLanguages("JAVA").build();
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(published));

        assertThatThrownBy(() -> assessmentService.publishAssessment(companyId, assessmentId))
                .isInstanceOf(AssessmentConflictException.class);
        verifyNoInteractions(assessmentEligibilityService);
    }

    @Test
    void getAssessment_shouldThrow_whenAssessmentBelongsToAnotherCompany() {
        UUID assessmentId = UUID.randomUUID();
        Assessment other = Assessment.builder().id(assessmentId).companyId(UUID.randomUUID()).jobId(jobId)
                .status(AssessmentStatus.DRAFT).allowedLanguages("JAVA").build();
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> assessmentService.updateAssessment(companyId, assessmentId,
                UpdateAssessmentRequest.builder().name("x").type(AssessmentType.CODING)
                        .allowedLanguages(List.of(ProgrammingLanguage.JAVA)).difficulty(Difficulty.EASY)
                        .durationMinutes(10).questionCount(1).passingScorePercent(50).totalMarks(100).build()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private CreateAssessmentRequest.CreateAssessmentRequestBuilder baseRequest() {
        return CreateAssessmentRequest.builder()
                .name("Backend Assessment").jobId(jobId).type(AssessmentType.CODING)
                .allowedLanguages(List.of(ProgrammingLanguage.JAVA, ProgrammingLanguage.PYTHON))
                .difficulty(Difficulty.MEDIUM).durationMinutes(60).questionCount(2)
                .passingScorePercent(60).totalMarks(100);
    }

    private JobDetailDto jobDetail() {
        JobDetailDto dto = new JobDetailDto();
        dto.setId(jobId);
        dto.setTitle("Backend Engineer");
        return dto;
    }
}
