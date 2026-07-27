package com.cadence.interviewmanagementservice.service;

import com.cadence.interviewmanagementservice.constants.InterviewStatus;
import com.cadence.interviewmanagementservice.dto.request.CancelInterviewRequest;
import com.cadence.interviewmanagementservice.dto.request.RequestRescheduleRequest;
import com.cadence.interviewmanagementservice.dto.request.RescheduleInterviewRequest;
import com.cadence.interviewmanagementservice.dto.request.ScheduleInterviewRequest;
import com.cadence.interviewmanagementservice.dto.response.*;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface InterviewService {

    InterviewDetailResponse scheduleInterview(UUID companyId, UUID recruiterId, ScheduleInterviewRequest request);

    InterviewDetailResponse rescheduleInterview(UUID companyId, UUID interviewId, UUID actorId, RescheduleInterviewRequest request);

    InterviewDetailResponse cancelInterview(UUID companyId, UUID interviewId, UUID actorId, CancelInterviewRequest request);

    InterviewDetailResponse getInterview(UUID companyId, UUID interviewId);

    PagedResponse<InterviewListItemResponse> listInterviews(UUID companyId, InterviewStatus status, Pageable pageable);

    List<ActivityLogResponse> getActivityLog(UUID companyId, UUID interviewId);

    List<CandidateInterviewResponse> getCandidateInterviews(UUID candidateId, boolean upcomingOnly);

    CandidateInterviewResponse getCandidateInterviewDetail(UUID candidateId, UUID interviewId);

    void requestReschedule(UUID candidateId, UUID interviewId, RequestRescheduleRequest request);
}
