package com.cadence.interviewmanagementservice.service.impl;

import com.cadence.interviewmanagementservice.constants.InterviewStatus;
import com.cadence.interviewmanagementservice.constants.RecommendationType;
import com.cadence.interviewmanagementservice.constants.RoundType;
import com.cadence.interviewmanagementservice.dto.request.SubmitFeedbackRequest;
import com.cadence.interviewmanagementservice.entity.Interview;
import com.cadence.interviewmanagementservice.entity.InterviewFeedback;
import com.cadence.interviewmanagementservice.entity.InterviewPanelist;
import com.cadence.interviewmanagementservice.exception.AccessDeniedApiException;
import com.cadence.interviewmanagementservice.exception.InterviewConflictException;
import com.cadence.interviewmanagementservice.kafka.producer.InterviewManagementEventProducer;
import com.cadence.interviewmanagementservice.mapper.InterviewFeedbackMapper;
import com.cadence.interviewmanagementservice.mapper.InterviewFeedbackMapperImpl;
import com.cadence.interviewmanagementservice.repository.InterviewActivityLogRepository;
import com.cadence.interviewmanagementservice.repository.InterviewFeedbackRepository;
import com.cadence.interviewmanagementservice.repository.InterviewPanelistRepository;
import com.cadence.interviewmanagementservice.repository.InterviewRepository;
import com.cadence.interviewmanagementservice.service.CandidateTimelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InterviewFeedbackServiceImplTest {

    @Mock private InterviewRepository interviewRepository;
    @Mock private InterviewFeedbackRepository interviewFeedbackRepository;
    @Mock private InterviewPanelistRepository interviewPanelistRepository;
    @Mock private InterviewActivityLogRepository interviewActivityLogRepository;
    @Mock private InterviewManagementEventProducer eventProducer;
    @Mock private CandidateTimelineService candidateTimelineService;

    private final InterviewFeedbackMapper interviewFeedbackMapper = new InterviewFeedbackMapperImpl();

    private InterviewFeedbackServiceImpl feedbackService;

    private UUID companyId;
    private UUID interviewId;
    private UUID interviewerId;

    @BeforeEach
    void setUp() {
        feedbackService = new InterviewFeedbackServiceImpl(interviewRepository, interviewFeedbackRepository,
                interviewPanelistRepository, interviewActivityLogRepository, interviewFeedbackMapper, eventProducer,
                candidateTimelineService);
        companyId = UUID.randomUUID();
        interviewId = UUID.randomUUID();
        interviewerId = UUID.randomUUID();

        lenient().when(interviewFeedbackRepository.save(any(InterviewFeedback.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Interview interview(InterviewStatus status) {
        return Interview.builder().id(interviewId).companyId(companyId).jobId(UUID.randomUUID())
                .applicationId(UUID.randomUUID()).candidateId(UUID.randomUUID()).roundType(RoundType.TECHNICAL)
                .status(status).scheduledDate(LocalDate.now()).scheduledTime(LocalTime.of(10, 0)).durationMinutes(45).build();
    }

    private SubmitFeedbackRequest feedbackRequest() {
        return SubmitFeedbackRequest.builder().communicationScore(8).technicalScore(7).cultureFitScore(9)
                .strengths("Strong fundamentals").weaknesses("Needs more system design depth")
                .recommendation(RecommendationType.PROCEED).build();
    }

    @Test
    void submitFeedback_shouldPersistMarkPanelistAndCompleteInterview_whenCallerIsPanelist() {
        when(interviewRepository.findById(interviewId)).thenReturn(Optional.of(interview(InterviewStatus.SCHEDULED)));
        when(interviewFeedbackRepository.findByInterviewIdAndInterviewerId(interviewId, interviewerId)).thenReturn(Optional.empty());
        when(interviewPanelistRepository.existsByInterviewIdAndInterviewerId(interviewId, interviewerId)).thenReturn(true);
        InterviewPanelist panelist = InterviewPanelist.builder().interviewId(interviewId).interviewerId(interviewerId).build();
        when(interviewPanelistRepository.findAllByInterviewId(interviewId)).thenReturn(List.of(panelist));

        var response = feedbackService.submitFeedback(companyId, interviewId, interviewerId, false, feedbackRequest());

        assertThat(response.getRecommendation()).isEqualTo(RecommendationType.PROCEED);
        verify(interviewRepository).save(argThat(i -> i.getStatus() == InterviewStatus.COMPLETED));
        verify(interviewPanelistRepository).save(argThat(InterviewPanelist::isFeedbackSubmitted));
        verify(eventProducer).publishInterviewCompleted(any());
        verify(eventProducer).publishApplicationInterviewCompleted(any());
        verify(candidateTimelineService).markStageDone(any(), any(), any());
    }

    @Test
    void submitFeedback_shouldThrow_whenCallerIsNeitherPanelistNorRecruitingRole() {
        when(interviewRepository.findById(interviewId)).thenReturn(Optional.of(interview(InterviewStatus.SCHEDULED)));
        when(interviewFeedbackRepository.findByInterviewIdAndInterviewerId(interviewId, interviewerId)).thenReturn(Optional.empty());
        when(interviewPanelistRepository.existsByInterviewIdAndInterviewerId(interviewId, interviewerId)).thenReturn(false);

        assertThatThrownBy(() -> feedbackService.submitFeedback(companyId, interviewId, interviewerId, false, feedbackRequest()))
                .isInstanceOf(AccessDeniedApiException.class);
    }

    @Test
    void submitFeedback_shouldThrow_whenAlreadySubmittedByThisInterviewer() {
        when(interviewRepository.findById(interviewId)).thenReturn(Optional.of(interview(InterviewStatus.SCHEDULED)));
        when(interviewFeedbackRepository.findByInterviewIdAndInterviewerId(interviewId, interviewerId))
                .thenReturn(Optional.of(InterviewFeedback.builder().build()));

        assertThatThrownBy(() -> feedbackService.submitFeedback(companyId, interviewId, interviewerId, true, feedbackRequest()))
                .isInstanceOf(InterviewConflictException.class);
    }

    @Test
    void submitFeedback_shouldThrow_whenInterviewCancelled() {
        when(interviewRepository.findById(interviewId)).thenReturn(Optional.of(interview(InterviewStatus.CANCELLED)));

        assertThatThrownBy(() -> feedbackService.submitFeedback(companyId, interviewId, interviewerId, true, feedbackRequest()))
                .isInstanceOf(InterviewConflictException.class);
    }
}
