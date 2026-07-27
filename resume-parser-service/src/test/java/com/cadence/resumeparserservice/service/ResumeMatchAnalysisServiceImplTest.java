package com.cadence.resumeparserservice.service;

import com.cadence.resumeparserservice.constants.ParsingStatus;
import com.cadence.resumeparserservice.constants.ResumeMatchStatus;
import com.cadence.resumeparserservice.entity.ParsedResume;
import com.cadence.resumeparserservice.entity.ResumeMatch;
import com.cadence.resumeparserservice.exception.ParsingConflictException;
import com.cadence.resumeparserservice.exception.ResourceNotFoundException;
import com.cadence.resumeparserservice.feign.ApplicationServiceClient;
import com.cadence.resumeparserservice.feign.dto.ApplicationSummaryDto;
import com.cadence.resumeparserservice.feign.dto.FeignApiResponse;
import com.cadence.resumeparserservice.kafka.event.ApplicationCreatedEvent;
import com.cadence.resumeparserservice.repository.ParsedResumeRepository;
import com.cadence.resumeparserservice.repository.ResumeMatchRepository;
import com.cadence.resumeparserservice.service.impl.ResumeMatchAnalysisPipelineRunner;
import com.cadence.resumeparserservice.service.impl.ResumeMatchAnalysisServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeMatchAnalysisServiceImplTest {

    @Mock private ResumeMatchRepository resumeMatchRepository;
    @Mock private ParsedResumeRepository parsedResumeRepository;
    @Mock private ApplicationServiceClient applicationServiceClient;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private ResumeMatchAnalysisPipelineRunner pipelineRunner;

    @InjectMocks
    private ResumeMatchAnalysisServiceImpl resumeMatchAnalysisService;

    private UUID applicationId;
    private UUID jobId;
    private UUID candidateId;
    private UUID resumeId;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(resumeMatchAnalysisService, "lockTtlSeconds", 300L);
        applicationId = UUID.randomUUID();
        jobId = UUID.randomUUID();
        candidateId = UUID.randomUUID();
        resumeId = UUID.randomUUID();
        // lenient: only the tests that actually reach advanceOrQueue's save trigger this
        lenient().when(resumeMatchRepository.save(any(ResumeMatch.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void handleApplicationCreated_shouldIgnoreDuplicate_whenResumeMatchAlreadyExists() {
        when(resumeMatchRepository.findByApplicationId(applicationId))
                .thenReturn(Optional.of(ResumeMatch.builder().applicationId(applicationId).build()));

        resumeMatchAnalysisService.handleApplicationCreated(event(resumeId));

        verify(resumeMatchRepository, never()).save(any());
        verifyNoInteractions(pipelineRunner);
    }

    @Test
    void handleApplicationCreated_shouldParkAsAwaitingResume_whenResumeIdNull() {
        when(resumeMatchRepository.findByApplicationId(applicationId)).thenReturn(Optional.empty());

        resumeMatchAnalysisService.handleApplicationCreated(event(null));

        verify(resumeMatchRepository).save(argThat(rm -> rm.getStatus() == ResumeMatchStatus.AWAITING_RESUME
                && rm.getResumeId() == null));
        verifyNoInteractions(pipelineRunner, redisTemplate);
    }

    @Test
    void handleApplicationCreated_shouldParkAsAwaitingParse_whenResumeNotYetParsed() {
        when(resumeMatchRepository.findByApplicationId(applicationId)).thenReturn(Optional.empty());
        when(parsedResumeRepository.findByResumeId(resumeId)).thenReturn(Optional.empty());

        resumeMatchAnalysisService.handleApplicationCreated(event(resumeId));

        // atLeastOnce -- the same mutable ResumeMatch instance is saved twice
        // (once as AWAITING_RESUME on creation, once as AWAITING_PARSE from
        // advanceOrQueue), and Mockito's recorded argument is a live
        // reference, so both invocations reflect the final mutated state.
        verify(resumeMatchRepository, atLeastOnce()).save(argThat(rm -> rm.getStatus() == ResumeMatchStatus.AWAITING_PARSE));
        verifyNoInteractions(pipelineRunner);
    }

    @Test
    void handleApplicationCreated_shouldAnalyze_whenResumeAlreadyParsed() {
        when(resumeMatchRepository.findByApplicationId(applicationId)).thenReturn(Optional.empty());
        ParsedResume parsedResume = ParsedResume.builder().resumeId(resumeId).candidateId(candidateId)
                .checksum("c").status(ParsingStatus.PARSED).build();
        when(parsedResumeRepository.findByResumeId(resumeId)).thenReturn(Optional.of(parsedResume));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        resumeMatchAnalysisService.handleApplicationCreated(event(resumeId));

        verify(resumeMatchRepository, atLeastOnce()).save(argThat(rm -> rm.getStatus() == ResumeMatchStatus.ANALYZING));
        verify(pipelineRunner).run(any(), anyString());
    }

    @Test
    void handleApplicationCreated_shouldSkip_whenLockAlreadyHeld() {
        when(resumeMatchRepository.findByApplicationId(applicationId)).thenReturn(Optional.empty());
        ParsedResume parsedResume = ParsedResume.builder().resumeId(resumeId).candidateId(candidateId)
                .checksum("c").status(ParsingStatus.PARSED).build();
        when(parsedResumeRepository.findByResumeId(resumeId)).thenReturn(Optional.of(parsedResume));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        resumeMatchAnalysisService.handleApplicationCreated(event(resumeId));

        verifyNoInteractions(pipelineRunner);
    }

    @Test
    void recalculate_shouldThrow_whenNotFound() {
        when(resumeMatchRepository.findByApplicationId(applicationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resumeMatchAnalysisService.recalculate(applicationId))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(pipelineRunner);
    }

    @Test
    void recalculate_shouldThrow_whenAlreadyAnalyzing() {
        ResumeMatch resumeMatch = ResumeMatch.builder().applicationId(applicationId).jobId(jobId)
                .status(ResumeMatchStatus.ANALYZING).build();
        when(resumeMatchRepository.findByApplicationId(applicationId)).thenReturn(Optional.of(resumeMatch));

        assertThatThrownBy(() -> resumeMatchAnalysisService.recalculate(applicationId))
                .isInstanceOf(ParsingConflictException.class);

        verifyNoInteractions(pipelineRunner);
    }

    @Test
    void recalculate_shouldRemainAwaitingResume_whenResumeIdStillNull() {
        ResumeMatch resumeMatch = ResumeMatch.builder().applicationId(applicationId).jobId(jobId)
                .status(ResumeMatchStatus.AWAITING_RESUME).resumeId(null).build();
        when(resumeMatchRepository.findByApplicationId(applicationId)).thenReturn(Optional.of(resumeMatch));
        FeignApiResponse<List<ApplicationSummaryDto>> response = new FeignApiResponse<>(true, "OK", List.of());
        when(applicationServiceClient.getApplicationsByJob(jobId)).thenReturn(response);

        resumeMatchAnalysisService.recalculate(applicationId);

        assertThat(resumeMatch.getStatus()).isEqualTo(ResumeMatchStatus.AWAITING_RESUME);
        verifyNoInteractions(pipelineRunner);
    }

    @Test
    void recalculate_shouldResolveResumeIdAndAnalyze_whenApplicationServiceHasIt() {
        ResumeMatch resumeMatch = ResumeMatch.builder().applicationId(applicationId).jobId(jobId)
                .status(ResumeMatchStatus.AWAITING_RESUME).resumeId(null).attemptCount(0).build();
        when(resumeMatchRepository.findByApplicationId(applicationId)).thenReturn(Optional.of(resumeMatch));

        ApplicationSummaryDto dto = new ApplicationSummaryDto();
        dto.setId(applicationId);
        dto.setResumeId(resumeId);
        FeignApiResponse<List<ApplicationSummaryDto>> response = new FeignApiResponse<>(true, "OK", List.of(dto));
        when(applicationServiceClient.getApplicationsByJob(jobId)).thenReturn(response);

        ParsedResume parsedResume = ParsedResume.builder().resumeId(resumeId).candidateId(candidateId)
                .checksum("c").status(ParsingStatus.PARSED).build();
        when(parsedResumeRepository.findByResumeId(resumeId)).thenReturn(Optional.of(parsedResume));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        resumeMatchAnalysisService.recalculate(applicationId);

        assertThat(resumeMatch.getResumeId()).isEqualTo(resumeId);
        assertThat(resumeMatch.getAttemptCount()).isEqualTo(1);
        assertThat(resumeMatch.getStatus()).isEqualTo(ResumeMatchStatus.ANALYZING);
        verify(pipelineRunner).run(any(), anyString());
    }

    @Test
    void onResumeParsed_shouldAdvanceEveryAwaitingRow() {
        ResumeMatch rm1 = ResumeMatch.builder().applicationId(UUID.randomUUID()).jobId(jobId).resumeId(resumeId)
                .status(ResumeMatchStatus.AWAITING_PARSE).build();
        ResumeMatch rm2 = ResumeMatch.builder().applicationId(UUID.randomUUID()).jobId(jobId).resumeId(resumeId)
                .status(ResumeMatchStatus.AWAITING_PARSE).build();
        when(resumeMatchRepository.findAllByResumeIdAndStatus(resumeId, ResumeMatchStatus.AWAITING_PARSE))
                .thenReturn(List.of(rm1, rm2));

        ParsedResume parsedResume = ParsedResume.builder().resumeId(resumeId).candidateId(candidateId)
                .checksum("c").status(ParsingStatus.PARSED).build();
        when(parsedResumeRepository.findByResumeId(resumeId)).thenReturn(Optional.of(parsedResume));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        resumeMatchAnalysisService.onResumeParsed(resumeId);

        assertThat(rm1.getStatus()).isEqualTo(ResumeMatchStatus.ANALYZING);
        assertThat(rm2.getStatus()).isEqualTo(ResumeMatchStatus.ANALYZING);
        verify(pipelineRunner, times(2)).run(any(), anyString());
    }

    private ApplicationCreatedEvent event(UUID resumeId) {
        return ApplicationCreatedEvent.builder()
                .applicationId(applicationId).companyId(UUID.randomUUID()).jobId(jobId)
                .candidateId(candidateId).resumeId(resumeId).build();
    }
}
