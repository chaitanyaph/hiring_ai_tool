package com.cadence.resumeparserservice.service;

import com.cadence.resumeparserservice.constants.ParsingStatus;
import com.cadence.resumeparserservice.entity.ParsedResume;
import com.cadence.resumeparserservice.exception.ParsingConflictException;
import com.cadence.resumeparserservice.exception.ResourceNotFoundException;
import com.cadence.resumeparserservice.repository.ParsedResumeRepository;
import com.cadence.resumeparserservice.repository.ParserLogRepository;
import com.cadence.resumeparserservice.service.impl.ResumeParsingPipelineRunner;
import com.cadence.resumeparserservice.service.impl.ResumeParsingServiceImpl;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeParsingServiceImplTest {

    @Mock private ParsedResumeRepository parsedResumeRepository;
    @Mock private ParserLogRepository parserLogRepository;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private ResumeParsingPipelineRunner pipelineRunner;

    @InjectMocks
    private ResumeParsingServiceImpl resumeParsingService;

    private UUID resumeId;
    private UUID candidateId;
    private String checksum;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(resumeParsingService, "lockTtlSeconds", 300L);
        resumeId = UUID.randomUUID();
        candidateId = UUID.randomUUID();
        checksum = "abc123";
        // lenient: only the "happy path" tests below actually trigger a save()
        lenient().when(parsedResumeRepository.save(any(ParsedResume.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void processResume_shouldSkip_whenAlreadyParsedWithSameChecksum() {
        when(parsedResumeRepository.existsByResumeIdAndChecksumAndStatus(resumeId, checksum, ParsingStatus.PARSED))
                .thenReturn(true);

        resumeParsingService.processResume(resumeId, candidateId, checksum);

        verifyNoInteractions(redisTemplate, pipelineRunner);
        verify(parsedResumeRepository, never()).save(any());
    }

    @Test
    void processResume_shouldSkip_whenIdempotencyLockAlreadyHeld() {
        when(parsedResumeRepository.existsByResumeIdAndChecksumAndStatus(any(), any(), any())).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        resumeParsingService.processResume(resumeId, candidateId, checksum);

        verify(pipelineRunner, never()).run(any(), any());
        verify(parsedResumeRepository, never()).save(any());
    }

    @Test
    void processResume_shouldQueueAndDispatch_whenLockAcquired() {
        when(parsedResumeRepository.existsByResumeIdAndChecksumAndStatus(any(), any(), any())).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(parsedResumeRepository.findByResumeId(resumeId)).thenReturn(Optional.empty());

        resumeParsingService.processResume(resumeId, candidateId, checksum);

        verify(parsedResumeRepository).save(argThat(pr -> pr.getStatus() == ParsingStatus.QUEUED
                && pr.getResumeId().equals(resumeId) && pr.getCandidateId().equals(candidateId)));
        verify(pipelineRunner).run(any(), anyString());
    }

    @Test
    void retryParsing_shouldThrow_whenNoRecordExists() {
        when(parsedResumeRepository.findByResumeId(resumeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resumeParsingService.retryParsing(resumeId))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(pipelineRunner);
    }

    @Test
    void retryParsing_shouldThrow_whenStatusIsNotFailed() {
        ParsedResume parsedResume = ParsedResume.builder()
                .resumeId(resumeId).candidateId(candidateId).checksum(checksum).status(ParsingStatus.PARSED).build();
        when(parsedResumeRepository.findByResumeId(resumeId)).thenReturn(Optional.of(parsedResume));

        assertThatThrownBy(() -> resumeParsingService.retryParsing(resumeId))
                .isInstanceOf(ParsingConflictException.class);

        verifyNoInteractions(pipelineRunner);
    }

    @Test
    void retryParsing_shouldThrow_whenAlreadyInProgress() {
        ParsedResume parsedResume = ParsedResume.builder()
                .resumeId(resumeId).candidateId(candidateId).checksum(checksum).status(ParsingStatus.FAILED).build();
        when(parsedResumeRepository.findByResumeId(resumeId)).thenReturn(Optional.of(parsedResume));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        assertThatThrownBy(() -> resumeParsingService.retryParsing(resumeId))
                .isInstanceOf(ParsingConflictException.class);

        verifyNoInteractions(pipelineRunner);
    }

    @Test
    void retryParsing_shouldIncrementAttemptAndDispatch_whenFailedAndLockAcquired() {
        ParsedResume parsedResume = ParsedResume.builder()
                .resumeId(resumeId).candidateId(candidateId).checksum(checksum)
                .status(ParsingStatus.FAILED).attemptCount(1).build();
        when(parsedResumeRepository.findByResumeId(resumeId)).thenReturn(Optional.of(parsedResume));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        resumeParsingService.retryParsing(resumeId);

        assertThat(parsedResume.getAttemptCount()).isEqualTo(2);
        assertThat(parsedResume.getStatus()).isEqualTo(ParsingStatus.QUEUED);
        verify(pipelineRunner).run(any(), anyString());
    }

    @Test
    void handleCandidateDeleted_shouldDeleteAllParsedResumesForCandidate() {
        ParsedResume p1 = ParsedResume.builder().resumeId(UUID.randomUUID()).candidateId(candidateId).checksum("a").status(ParsingStatus.PARSED).build();
        ParsedResume p2 = ParsedResume.builder().resumeId(UUID.randomUUID()).candidateId(candidateId).checksum("b").status(ParsingStatus.FAILED).build();
        when(parsedResumeRepository.findAllByCandidateId(candidateId)).thenReturn(List.of(p1, p2));

        resumeParsingService.handleCandidateDeleted(candidateId);

        verify(parsedResumeRepository).deleteAll(List.of(p1, p2));
    }

    @Test
    void handleCandidateDeleted_shouldDoNothing_whenNoRecordsExist() {
        when(parsedResumeRepository.findAllByCandidateId(candidateId)).thenReturn(List.of());

        resumeParsingService.handleCandidateDeleted(candidateId);

        verify(parsedResumeRepository, never()).deleteAll(anyList());
    }
}
