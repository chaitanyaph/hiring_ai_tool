package com.cadence.interviewmanagementservice.service.impl;

import com.cadence.interviewmanagementservice.constants.ActivityEventType;
import com.cadence.interviewmanagementservice.constants.InterviewStatus;
import com.cadence.interviewmanagementservice.dto.request.CancelInterviewRequest;
import com.cadence.interviewmanagementservice.dto.request.RequestRescheduleRequest;
import com.cadence.interviewmanagementservice.dto.request.RescheduleInterviewRequest;
import com.cadence.interviewmanagementservice.dto.request.ScheduleInterviewRequest;
import com.cadence.interviewmanagementservice.dto.response.*;
import com.cadence.interviewmanagementservice.entity.Interview;
import com.cadence.interviewmanagementservice.entity.InterviewActivityLog;
import com.cadence.interviewmanagementservice.entity.InterviewPanelist;
import com.cadence.interviewmanagementservice.exception.AccessDeniedApiException;
import com.cadence.interviewmanagementservice.exception.ErrorCode;
import com.cadence.interviewmanagementservice.exception.InterviewConflictException;
import com.cadence.interviewmanagementservice.exception.ResourceNotFoundException;
import com.cadence.interviewmanagementservice.feign.CandidateServiceClient;
import com.cadence.interviewmanagementservice.feign.CompanyServiceClient;
import com.cadence.interviewmanagementservice.feign.JobServiceClient;
import com.cadence.interviewmanagementservice.kafka.event.InterviewCancelledEvent;
import com.cadence.interviewmanagementservice.kafka.event.InterviewRescheduledEvent;
import com.cadence.interviewmanagementservice.kafka.event.InterviewScheduledEvent;
import com.cadence.interviewmanagementservice.kafka.producer.InterviewManagementEventProducer;
import com.cadence.interviewmanagementservice.mapper.InterviewMapper;
import com.cadence.interviewmanagementservice.repository.InterviewActivityLogRepository;
import com.cadence.interviewmanagementservice.repository.InterviewPanelistRepository;
import com.cadence.interviewmanagementservice.repository.InterviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements com.cadence.interviewmanagementservice.service.InterviewService {

    private final InterviewRepository interviewRepository;
    private final InterviewPanelistRepository interviewPanelistRepository;
    private final InterviewActivityLogRepository interviewActivityLogRepository;
    private final InterviewMapper interviewMapper;
    private final CandidateServiceClient candidateServiceClient;
    private final JobServiceClient jobServiceClient;
    private final CompanyServiceClient companyServiceClient;
    private final InterviewManagementEventProducer eventProducer;

    @Override
    @Transactional
    public InterviewDetailResponse scheduleInterview(UUID companyId, UUID recruiterId, ScheduleInterviewRequest request) {
        Interview interview = Interview.builder()
                .companyId(companyId)
                .jobId(request.getJobId())
                .applicationId(request.getApplicationId())
                .candidateId(request.getCandidateId())
                .interviewRoundId(request.getInterviewRoundId())
                .roundType(request.getRoundType())
                .status(InterviewStatus.SCHEDULED)
                .scheduledDate(request.getScheduledDate())
                .scheduledTime(request.getScheduledTime())
                .durationMinutes(request.getDurationMinutes())
                .autoGenerateMeetLink(request.isAutoGenerateMeetLink())
                .notifyCandidateByEmail(request.isNotifyCandidateByEmail())
                .notesForPanel(request.getNotesForPanel())
                .createdByRecruiterId(recruiterId)
                .build();
        if (request.isAutoGenerateMeetLink()) {
            interview.setMeetingLink(generatePlaceholderMeetLink());
        }
        interview = interviewRepository.save(interview);

        for (UUID interviewerId : request.getPanelistIds()) {
            interviewPanelistRepository.save(InterviewPanelist.builder()
                    .interviewId(interview.getId())
                    .interviewerId(interviewerId)
                    .build());
        }

        logActivity(interview.getId(), ActivityEventType.SCHEDULED, recruiterId,
                "Scheduled for " + interview.getScheduledDate() + " " + interview.getScheduledTime());

        eventProducer.publishInterviewScheduled(InterviewScheduledEvent.builder()
                .interviewId(interview.getId())
                .applicationId(interview.getApplicationId())
                .candidateId(interview.getCandidateId())
                .jobId(interview.getJobId())
                .roundType(interview.getRoundType())
                .scheduledDate(interview.getScheduledDate())
                .scheduledTime(interview.getScheduledTime())
                .build());

        // NOTE: no Notification Service exists yet (same gap flagged in
        // ai-interview-service/coding-assessment-service) -- notify-by-email
        // is validated/stored only, no email is actually sent.

        return toDetailResponse(interview);
    }

    @Override
    @Transactional
    public InterviewDetailResponse rescheduleInterview(UUID companyId, UUID interviewId, UUID actorId, RescheduleInterviewRequest request) {
        Interview interview = findOwnedInterview(companyId, interviewId);
        requireNotTerminal(interview);

        LocalDate oldDate = interview.getScheduledDate();
        var oldTime = interview.getScheduledTime();

        interview.setScheduledDate(request.getScheduledDate());
        interview.setScheduledTime(request.getScheduledTime());
        interview.setDurationMinutes(request.getDurationMinutes());
        interview.setRescheduleReason(request.getRescheduleReason());
        interview.setStatus(InterviewStatus.RESCHEDULED);
        interview = interviewRepository.save(interview);

        logActivity(interview.getId(), ActivityEventType.RESCHEDULED, actorId,
                "From " + oldDate + " " + oldTime + " to " + interview.getScheduledDate() + " " + interview.getScheduledTime());

        eventProducer.publishInterviewRescheduled(InterviewRescheduledEvent.builder()
                .interviewId(interview.getId())
                .applicationId(interview.getApplicationId())
                .newScheduledDate(interview.getScheduledDate())
                .newScheduledTime(interview.getScheduledTime())
                .reason(interview.getRescheduleReason())
                .build());

        return toDetailResponse(interview);
    }

    @Override
    @Transactional
    public InterviewDetailResponse cancelInterview(UUID companyId, UUID interviewId, UUID actorId, CancelInterviewRequest request) {
        Interview interview = findOwnedInterview(companyId, interviewId);
        requireNotTerminal(interview);

        interview.setStatus(InterviewStatus.CANCELLED);
        interview.setCancelReason(request.getCancelReason());
        interview = interviewRepository.save(interview);

        logActivity(interview.getId(), ActivityEventType.CANCELLED, actorId, request.getCancelReason());

        eventProducer.publishInterviewCancelled(InterviewCancelledEvent.builder()
                .interviewId(interview.getId())
                .applicationId(interview.getApplicationId())
                .reason(interview.getCancelReason())
                .build());

        return toDetailResponse(interview);
    }

    @Override
    public InterviewDetailResponse getInterview(UUID companyId, UUID interviewId) {
        return toDetailResponse(findOwnedInterview(companyId, interviewId));
    }

    @Override
    public PagedResponse<InterviewListItemResponse> listInterviews(UUID companyId, InterviewStatus status, Pageable pageable) {
        Page<Interview> page = status != null
                ? interviewRepository.findAllByCompanyIdAndStatus(companyId, status, pageable)
                : interviewRepository.findAllByCompanyId(companyId, pageable);
        Page<InterviewListItemResponse> mapped = page.map(this::toListItemResponse);
        return PagedResponse.from(mapped);
    }

    @Override
    public List<ActivityLogResponse> getActivityLog(UUID companyId, UUID interviewId) {
        findOwnedInterview(companyId, interviewId);
        return interviewActivityLogRepository.findAllByInterviewIdOrderByOccurredAtDesc(interviewId).stream()
                .map(log -> ActivityLogResponse.builder()
                        .eventType(log.getEventType())
                        .actorId(log.getActorId())
                        .occurredAt(log.getOccurredAt())
                        .details(log.getDetails())
                        .build())
                .toList();
    }

    @Override
    public List<CandidateInterviewResponse> getCandidateInterviews(UUID candidateId, boolean upcomingOnly) {
        List<Interview> interviews = interviewRepository.findAllByCandidateIdOrderByScheduledDateDescScheduledTimeDesc(candidateId);
        return interviews.stream()
                .filter(i -> !upcomingOnly || isUpcoming(i))
                .map(this::toCandidateResponse)
                .toList();
    }

    @Override
    public CandidateInterviewResponse getCandidateInterviewDetail(UUID candidateId, UUID interviewId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.INTERVIEW_NOT_FOUND, "Interview not found: " + interviewId));
        if (!interview.getCandidateId().equals(candidateId)) {
            throw new AccessDeniedApiException("This interview does not belong to you");
        }
        return toCandidateResponse(interview);
    }

    @Override
    @Transactional
    public void requestReschedule(UUID candidateId, UUID interviewId, RequestRescheduleRequest request) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.INTERVIEW_NOT_FOUND, "Interview not found: " + interviewId));
        if (!interview.getCandidateId().equals(candidateId)) {
            throw new AccessDeniedApiException("This interview does not belong to you");
        }
        // Mirrors the Figma's mockToast-only "Request reschedule" (§A9):
        // logged for the recruiter to see, not auto-actioned.
        logActivity(interviewId, ActivityEventType.RESCHEDULED, candidateId,
                "Candidate requested reschedule: " + (request.getReason() == null ? "no reason given" : request.getReason()));
    }

    private void requireNotTerminal(Interview interview) {
        if (interview.getStatus() == InterviewStatus.COMPLETED) {
            throw new InterviewConflictException(ErrorCode.INTERVIEW_ALREADY_COMPLETED, "Cannot modify a completed interview");
        }
        if (interview.getStatus() == InterviewStatus.CANCELLED) {
            throw new InterviewConflictException(ErrorCode.INTERVIEW_ALREADY_CANCELLED, "Cannot modify a cancelled interview");
        }
    }

    private boolean isUpcoming(Interview interview) {
        if (interview.getStatus() == InterviewStatus.COMPLETED || interview.getStatus() == InterviewStatus.CANCELLED) {
            return false;
        }
        return LocalDateTime.of(interview.getScheduledDate(), interview.getScheduledTime()).isAfter(LocalDateTime.now());
    }

    private void logActivity(UUID interviewId, ActivityEventType type, UUID actorId, String details) {
        interviewActivityLogRepository.save(InterviewActivityLog.builder()
                .interviewId(interviewId)
                .eventType(type)
                .actorId(actorId)
                .details(details)
                .build());
    }

    private String generatePlaceholderMeetLink() {
        String chars = "abcdefghijklmnopqrstuvwxyz";
        StringBuilder sb = new StringBuilder("https://meet.google.com/");
        int[] groups = {3, 4, 3};
        for (int g = 0; g < groups.length; g++) {
            if (g > 0) sb.append('-');
            for (int i = 0; i < groups[g]; i++) {
                sb.append(chars.charAt(ThreadLocalRandom.current().nextInt(chars.length())));
            }
        }
        return sb.toString();
    }

    private Interview findOwnedInterview(UUID companyId, UUID interviewId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.INTERVIEW_NOT_FOUND, "Interview not found: " + interviewId));
        if (!interview.getCompanyId().equals(companyId)) {
            throw new ResourceNotFoundException(ErrorCode.INTERVIEW_NOT_FOUND, "Interview not found: " + interviewId);
        }
        return interview;
    }

    private List<PanelistResponse> panelists(UUID interviewId) {
        return interviewPanelistRepository.findAllByInterviewId(interviewId).stream()
                .map(p -> PanelistResponse.builder()
                        .interviewerId(p.getInterviewerId())
                        .interviewerRole(p.getInterviewerRole())
                        .feedbackSubmitted(p.isFeedbackSubmitted())
                        .build())
                .toList();
    }

    private InterviewListItemResponse toListItemResponse(Interview interview) {
        InterviewListItemResponse response = interviewMapper.toListItemResponse(interview);
        response.setCandidateName(safeCandidateName(interview.getCandidateId()));
        response.setJobTitle(safeJobTitle(interview.getJobId()));
        response.setPanelists(panelists(interview.getId()));
        response.setFeedbackSubmitted(panelists(interview.getId()).stream().anyMatch(PanelistResponse::isFeedbackSubmitted));
        return response;
    }

    private InterviewDetailResponse toDetailResponse(Interview interview) {
        InterviewDetailResponse response = interviewMapper.toDetailResponse(interview);
        response.setCandidateName(safeCandidateName(interview.getCandidateId()));
        response.setJobTitle(safeJobTitle(interview.getJobId()));
        response.setCompanyName(safeCompanyName(interview.getCompanyId()));
        response.setPanelists(panelists(interview.getId()));
        response.setFeedbackSubmittable(interview.getStatus() != InterviewStatus.CANCELLED
                && interview.getStatus() != InterviewStatus.COMPLETED);
        return response;
    }

    private CandidateInterviewResponse toCandidateResponse(Interview interview) {
        return CandidateInterviewResponse.builder()
                .id(interview.getId())
                .jobTitle(safeJobTitle(interview.getJobId()))
                .companyName(safeCompanyName(interview.getCompanyId()))
                .roundType(interview.getRoundType())
                .status(interview.getStatus())
                .scheduledDate(interview.getScheduledDate())
                .scheduledTime(interview.getScheduledTime())
                .mode(interview.getMode())
                .meetingLink(interview.getMeetingLink())
                .interviewerNames(List.of())
                .upcoming(isUpcoming(interview))
                .build();
    }

    private String safeCandidateName(UUID candidateId) {
        try {
            return candidateServiceClient.getCandidateSummary(candidateId).getData().getFullName();
        } catch (Exception e) {
            return null;
        }
    }

    private String safeJobTitle(UUID jobId) {
        try {
            return jobServiceClient.getJobDetail(jobId).getData().getTitle();
        } catch (Exception e) {
            return null;
        }
    }

    private String safeCompanyName(UUID companyId) {
        try {
            return companyServiceClient.getCompany(companyId).getData().getCompanyName();
        } catch (Exception e) {
            return null;
        }
    }
}
