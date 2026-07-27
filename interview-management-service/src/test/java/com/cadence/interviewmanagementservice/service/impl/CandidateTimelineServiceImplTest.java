package com.cadence.interviewmanagementservice.service.impl;

import com.cadence.interviewmanagementservice.constants.HiringRecommendation;
import com.cadence.interviewmanagementservice.constants.TimelineStage;
import com.cadence.interviewmanagementservice.constants.TimelineStatus;
import com.cadence.interviewmanagementservice.entity.CandidateTimeline;
import com.cadence.interviewmanagementservice.exception.AccessDeniedApiException;
import com.cadence.interviewmanagementservice.kafka.event.CandidateRecommendedEvent;
import com.cadence.interviewmanagementservice.kafka.event.CodingAssessmentCompletedEvent;
import com.cadence.interviewmanagementservice.mapper.CandidateTimelineMapper;
import com.cadence.interviewmanagementservice.mapper.CandidateTimelineMapperImpl;
import com.cadence.interviewmanagementservice.repository.CandidateTimelineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandidateTimelineServiceImplTest {

    @Mock private CandidateTimelineRepository candidateTimelineRepository;

    private final CandidateTimelineMapper candidateTimelineMapper = new CandidateTimelineMapperImpl();

    private CandidateTimelineServiceImpl timelineService;

    private UUID applicationId;
    private UUID candidateId;

    @BeforeEach
    void setUp() {
        timelineService = new CandidateTimelineServiceImpl(candidateTimelineRepository, candidateTimelineMapper);
        applicationId = UUID.randomUUID();
        candidateId = UUID.randomUUID();
        lenient().when(candidateTimelineRepository.save(any(CandidateTimeline.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void handleCandidateRecommended_shouldUpsertAiInterviewStageAsDone() {
        when(candidateTimelineRepository.findByApplicationIdAndStage(applicationId, TimelineStage.AI_INTERVIEW))
                .thenReturn(Optional.empty());

        timelineService.handleCandidateRecommended(CandidateRecommendedEvent.builder()
                .applicationId(applicationId).candidateId(candidateId).jobId(UUID.randomUUID())
                .hiringRecommendation(HiringRecommendation.PROCEED).occurredAt(LocalDateTime.now()).build());

        verify(candidateTimelineRepository).save(argThat(row ->
                row.getStage() == TimelineStage.AI_INTERVIEW && row.getStatus() == TimelineStatus.DONE
                        && row.getApplicationId().equals(applicationId)));
    }

    @Test
    void handleCodingAssessmentCompleted_shouldUpsertCodingAssessmentStageWithScore() {
        when(candidateTimelineRepository.findByApplicationIdAndStage(applicationId, TimelineStage.CODING_ASSESSMENT))
                .thenReturn(Optional.empty());

        timelineService.handleCodingAssessmentCompleted(CodingAssessmentCompletedEvent.builder()
                .applicationId(applicationId).score(88).build());

        verify(candidateTimelineRepository).save(argThat(row ->
                row.getStage() == TimelineStage.CODING_ASSESSMENT && row.getScore().equals(88)));
    }

    @Test
    void getMyTimeline_shouldThrow_whenApplicationDoesNotBelongToCandidate() {
        CandidateTimeline row = CandidateTimeline.builder().applicationId(applicationId)
                .candidateId(UUID.randomUUID()).stage(TimelineStage.AI_INTERVIEW).status(TimelineStatus.DONE).build();
        when(candidateTimelineRepository.findAllByApplicationId(applicationId)).thenReturn(List.of(row));

        assertThatThrownBy(() -> timelineService.getMyTimeline(candidateId, applicationId))
                .isInstanceOf(AccessDeniedApiException.class);
    }

    @Test
    void getMyTimeline_shouldReturnRows_whenOwnedByCandidate() {
        CandidateTimeline row = CandidateTimeline.builder().applicationId(applicationId)
                .candidateId(candidateId).stage(TimelineStage.AI_INTERVIEW).status(TimelineStatus.DONE).build();
        when(candidateTimelineRepository.findAllByApplicationId(applicationId)).thenReturn(List.of(row));

        var result = timelineService.getMyTimeline(candidateId, applicationId);

        assertThat(result).hasSize(1);
    }
}
