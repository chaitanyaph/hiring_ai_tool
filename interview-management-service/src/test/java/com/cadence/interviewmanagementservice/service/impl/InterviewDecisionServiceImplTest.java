package com.cadence.interviewmanagementservice.service.impl;

import com.cadence.interviewmanagementservice.constants.DecisionType;
import com.cadence.interviewmanagementservice.constants.InterviewStatus;
import com.cadence.interviewmanagementservice.constants.RoundType;
import com.cadence.interviewmanagementservice.dto.request.RecruiterDecisionRequest;
import com.cadence.interviewmanagementservice.entity.Interview;
import com.cadence.interviewmanagementservice.exception.ResourceNotFoundException;
import com.cadence.interviewmanagementservice.kafka.producer.InterviewManagementEventProducer;
import com.cadence.interviewmanagementservice.repository.InterviewActivityLogRepository;
import com.cadence.interviewmanagementservice.repository.InterviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InterviewDecisionServiceImplTest {

    @Mock private InterviewRepository interviewRepository;
    @Mock private InterviewActivityLogRepository interviewActivityLogRepository;
    @Mock private InterviewManagementEventProducer eventProducer;

    private InterviewDecisionServiceImpl decisionService;

    private UUID companyId;
    private UUID interviewId;
    private UUID actorId;

    @BeforeEach
    void setUp() {
        decisionService = new InterviewDecisionServiceImpl(interviewRepository, interviewActivityLogRepository, eventProducer);
        companyId = UUID.randomUUID();
        interviewId = UUID.randomUUID();
        actorId = UUID.randomUUID();
    }

    private Interview interview() {
        return Interview.builder().id(interviewId).companyId(companyId).jobId(UUID.randomUUID())
                .applicationId(UUID.randomUUID()).candidateId(UUID.randomUUID()).roundType(RoundType.HR)
                .status(InterviewStatus.COMPLETED).scheduledDate(LocalDate.now()).scheduledTime(LocalTime.of(10, 0))
                .durationMinutes(30).build();
    }

    @Test
    void recordDecision_shouldPublishCandidateSelected_whenSelect() {
        when(interviewRepository.findById(interviewId)).thenReturn(Optional.of(interview()));

        decisionService.recordDecision(companyId, interviewId, actorId,
                RecruiterDecisionRequest.builder().decisionType(DecisionType.SELECT).build());

        verify(eventProducer).publishCandidateSelected(any());
        verify(interviewActivityLogRepository).save(any());
    }

    @Test
    void recordDecision_shouldPublishCandidateRejected_whenReject() {
        when(interviewRepository.findById(interviewId)).thenReturn(Optional.of(interview()));

        decisionService.recordDecision(companyId, interviewId, actorId,
                RecruiterDecisionRequest.builder().decisionType(DecisionType.REJECT).notes("Not a fit").build());

        verify(eventProducer).publishCandidateRejected(any());
    }

    @Test
    void recordDecision_shouldOnlyLog_whenHold() {
        when(interviewRepository.findById(interviewId)).thenReturn(Optional.of(interview()));

        decisionService.recordDecision(companyId, interviewId, actorId,
                RecruiterDecisionRequest.builder().decisionType(DecisionType.HOLD).build());

        verify(interviewActivityLogRepository).save(any());
        verifyNoInteractions(eventProducer);
    }

    @Test
    void recordDecision_shouldThrow_whenInterviewBelongsToAnotherCompany() {
        Interview other = interview();
        other.setCompanyId(UUID.randomUUID());
        when(interviewRepository.findById(interviewId)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> decisionService.recordDecision(companyId, interviewId, actorId,
                RecruiterDecisionRequest.builder().decisionType(DecisionType.SELECT).build()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
