package com.cadence.candidateservice.service;

import com.cadence.candidateservice.dto.response.SavedJobResponse;
import com.cadence.candidateservice.entity.SavedJob;
import com.cadence.candidateservice.exception.ResourceNotFoundException;
import com.cadence.candidateservice.kafka.producer.CandidateEventProducer;
import com.cadence.candidateservice.mapper.SavedJobMapper;
import com.cadence.candidateservice.repository.SavedJobRepository;
import com.cadence.candidateservice.security.CurrentUser;
import com.cadence.candidateservice.service.impl.SavedJobServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SavedJobServiceImplTest {

    @Mock private SavedJobRepository savedJobRepository;
    @Mock private SavedJobMapper savedJobMapper;
    @Mock private CandidateEventProducer eventProducer;

    @InjectMocks
    private SavedJobServiceImpl savedJobService;

    private CurrentUser candidate;
    private UUID jobId;

    @BeforeEach
    void setUp() {
        candidate = CurrentUser.builder().userId(UUID.randomUUID()).role("CANDIDATE").build();
        jobId = UUID.randomUUID();
    }

    @Test
    void saveJob_shouldBeIdempotent_whenAlreadySaved() {
        SavedJob existing = SavedJob.builder().id(UUID.randomUUID()).candidateId(candidate.getUserId()).jobId(jobId).build();
        when(savedJobRepository.findByCandidateIdAndJobId(candidate.getUserId(), jobId)).thenReturn(Optional.of(existing));
        when(savedJobMapper.toResponse(existing)).thenReturn(SavedJobResponse.builder().id(existing.getId()).jobId(jobId).build());

        savedJobService.saveJob(candidate, jobId);

        verify(savedJobRepository, never()).save(any());
        verify(eventProducer, never()).publishJobSaved(any());
    }

    @Test
    void saveJob_shouldCreateAndPublishEvent_whenNotAlreadySaved() {
        when(savedJobRepository.findByCandidateIdAndJobId(candidate.getUserId(), jobId)).thenReturn(Optional.empty());
        when(savedJobRepository.save(any(SavedJob.class))).thenAnswer(inv -> inv.getArgument(0));
        when(savedJobMapper.toResponse(any(SavedJob.class))).thenReturn(SavedJobResponse.builder().jobId(jobId).build());

        savedJobService.saveJob(candidate, jobId);

        verify(savedJobRepository).save(any(SavedJob.class));
        verify(eventProducer).publishJobSaved(any());
    }

    @Test
    void unsaveJob_shouldThrow_whenNotSaved() {
        when(savedJobRepository.existsByCandidateIdAndJobId(candidate.getUserId(), jobId)).thenReturn(false);

        assertThatThrownBy(() -> savedJobService.unsaveJob(candidate, jobId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(savedJobRepository, never()).deleteByCandidateIdAndJobId(any(), any());
    }
}
