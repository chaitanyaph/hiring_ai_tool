package com.cadence.interviewmanagementservice.service.impl;

import com.cadence.interviewmanagementservice.constants.InterviewStatus;
import com.cadence.interviewmanagementservice.constants.RoundType;
import com.cadence.interviewmanagementservice.dto.request.CancelInterviewRequest;
import com.cadence.interviewmanagementservice.dto.request.RescheduleInterviewRequest;
import com.cadence.interviewmanagementservice.dto.request.ScheduleInterviewRequest;
import com.cadence.interviewmanagementservice.entity.Interview;
import com.cadence.interviewmanagementservice.exception.InterviewConflictException;
import com.cadence.interviewmanagementservice.exception.ResourceNotFoundException;
import com.cadence.interviewmanagementservice.feign.CandidateServiceClient;
import com.cadence.interviewmanagementservice.feign.CompanyServiceClient;
import com.cadence.interviewmanagementservice.feign.JobServiceClient;
import com.cadence.interviewmanagementservice.kafka.producer.InterviewManagementEventProducer;
import com.cadence.interviewmanagementservice.mapper.InterviewMapper;
import com.cadence.interviewmanagementservice.mapper.InterviewMapperImpl;
import com.cadence.interviewmanagementservice.repository.InterviewActivityLogRepository;
import com.cadence.interviewmanagementservice.repository.InterviewPanelistRepository;
import com.cadence.interviewmanagementservice.repository.InterviewRepository;
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
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InterviewServiceImplTest {

    @Mock private InterviewRepository interviewRepository;
    @Mock private InterviewPanelistRepository interviewPanelistRepository;
    @Mock private InterviewActivityLogRepository interviewActivityLogRepository;
    @Mock private CandidateServiceClient candidateServiceClient;
    @Mock private JobServiceClient jobServiceClient;
    @Mock private CompanyServiceClient companyServiceClient;
    @Mock private InterviewManagementEventProducer eventProducer;

    private final InterviewMapper interviewMapper = new InterviewMapperImpl();

    private InterviewServiceImpl interviewService;

    private UUID companyId;
    private UUID recruiterId;
    private final AtomicReference<Interview> saved = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        interviewService = new InterviewServiceImpl(interviewRepository, interviewPanelistRepository,
                interviewActivityLogRepository, interviewMapper, candidateServiceClient, jobServiceClient,
                companyServiceClient, eventProducer);
        companyId = UUID.randomUUID();
        recruiterId = UUID.randomUUID();

        lenient().when(interviewRepository.save(any(Interview.class))).thenAnswer(inv -> {
            Interview i = inv.getArgument(0);
            saved.set(i);
            return i;
        });
        lenient().when(interviewRepository.findById(any())).thenAnswer(inv -> Optional.ofNullable(saved.get()));
        lenient().when(candidateServiceClient.getCandidateSummary(any())).thenThrow(new RuntimeException("unreachable in test"));
        lenient().when(jobServiceClient.getJobDetail(any())).thenThrow(new RuntimeException("unreachable in test"));
        lenient().when(companyServiceClient.getCompany(any())).thenThrow(new RuntimeException("unreachable in test"));
    }

    @Test
    void scheduleInterview_shouldCreateInterviewWithPanelistsAndPublishEvent() {
        UUID interviewerId = UUID.randomUUID();
        ScheduleInterviewRequest request = ScheduleInterviewRequest.builder()
                .applicationId(UUID.randomUUID()).jobId(UUID.randomUUID()).candidateId(UUID.randomUUID())
                .roundType(RoundType.TECHNICAL).scheduledDate(LocalDate.now().plusDays(1)).scheduledTime(LocalTime.of(15, 30))
                .durationMinutes(60).panelistIds(List.of(interviewerId)).autoGenerateMeetLink(true).notifyCandidateByEmail(true)
                .build();

        var response = interviewService.scheduleInterview(companyId, recruiterId, request);

        assertThat(response.getStatus()).isEqualTo(InterviewStatus.SCHEDULED);
        assertThat(saved.get().getMeetingLink()).startsWith("https://meet.google.com/");
        verify(interviewPanelistRepository).save(argThat(p -> p.getInterviewerId().equals(interviewerId)));
        verify(interviewActivityLogRepository).save(any());
        verify(eventProducer).publishInterviewScheduled(any());
    }

    @Test
    void rescheduleInterview_shouldUpdateDateTimeAndLogActivity() {
        UUID interviewId = UUID.randomUUID();
        Interview interview = Interview.builder().id(interviewId).companyId(companyId).jobId(UUID.randomUUID())
                .applicationId(UUID.randomUUID()).candidateId(UUID.randomUUID()).roundType(RoundType.TECHNICAL)
                .status(InterviewStatus.SCHEDULED).scheduledDate(LocalDate.now()).scheduledTime(LocalTime.of(10, 0))
                .durationMinutes(30).build();
        saved.set(interview);

        RescheduleInterviewRequest request = RescheduleInterviewRequest.builder()
                .scheduledDate(LocalDate.now().plusDays(2)).scheduledTime(LocalTime.of(11, 0)).durationMinutes(45)
                .rescheduleReason("Panel conflict").build();

        var response = interviewService.rescheduleInterview(companyId, interviewId, recruiterId, request);

        assertThat(response.getStatus()).isEqualTo(InterviewStatus.RESCHEDULED);
        assertThat(saved.get().getScheduledTime()).isEqualTo(LocalTime.of(11, 0));
        verify(eventProducer).publishInterviewRescheduled(any());
    }

    @Test
    void rescheduleInterview_shouldThrow_whenAlreadyCompleted() {
        UUID interviewId = UUID.randomUUID();
        Interview interview = Interview.builder().id(interviewId).companyId(companyId).jobId(UUID.randomUUID())
                .applicationId(UUID.randomUUID()).candidateId(UUID.randomUUID()).roundType(RoundType.HR)
                .status(InterviewStatus.COMPLETED).scheduledDate(LocalDate.now()).scheduledTime(LocalTime.of(10, 0))
                .durationMinutes(30).build();
        saved.set(interview);

        RescheduleInterviewRequest request = RescheduleInterviewRequest.builder()
                .scheduledDate(LocalDate.now().plusDays(1)).scheduledTime(LocalTime.NOON).durationMinutes(30).build();

        assertThatThrownBy(() -> interviewService.rescheduleInterview(companyId, interviewId, recruiterId, request))
                .isInstanceOf(InterviewConflictException.class);
    }

    @Test
    void cancelInterview_shouldSetCancelledStatusAndPublishEvent() {
        UUID interviewId = UUID.randomUUID();
        Interview interview = Interview.builder().id(interviewId).companyId(companyId).jobId(UUID.randomUUID())
                .applicationId(UUID.randomUUID()).candidateId(UUID.randomUUID()).roundType(RoundType.MANAGER)
                .status(InterviewStatus.SCHEDULED).scheduledDate(LocalDate.now()).scheduledTime(LocalTime.of(10, 0))
                .durationMinutes(30).build();
        saved.set(interview);

        var response = interviewService.cancelInterview(companyId, interviewId, recruiterId,
                CancelInterviewRequest.builder().cancelReason("Candidate unavailable").build());

        assertThat(response.getStatus()).isEqualTo(InterviewStatus.CANCELLED);
        verify(eventProducer).publishInterviewCancelled(any());
    }

    @Test
    void getInterview_shouldThrow_whenInterviewBelongsToAnotherCompany() {
        UUID interviewId = UUID.randomUUID();
        Interview other = Interview.builder().id(interviewId).companyId(UUID.randomUUID()).jobId(UUID.randomUUID())
                .applicationId(UUID.randomUUID()).candidateId(UUID.randomUUID()).roundType(RoundType.HR)
                .status(InterviewStatus.SCHEDULED).scheduledDate(LocalDate.now()).scheduledTime(LocalTime.of(10, 0))
                .durationMinutes(30).build();
        saved.set(other);

        assertThatThrownBy(() -> interviewService.getInterview(companyId, interviewId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
