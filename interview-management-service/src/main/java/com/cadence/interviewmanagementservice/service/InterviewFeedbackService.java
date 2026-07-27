package com.cadence.interviewmanagementservice.service;

import com.cadence.interviewmanagementservice.dto.request.SubmitFeedbackRequest;
import com.cadence.interviewmanagementservice.dto.response.InterviewFeedbackResponse;

import java.util.List;
import java.util.UUID;

public interface InterviewFeedbackService {

    InterviewFeedbackResponse submitFeedback(UUID companyId, UUID interviewId, UUID interviewerId,
                                              boolean callerIsRecruitingRole, SubmitFeedbackRequest request);

    List<InterviewFeedbackResponse> getFeedback(UUID companyId, UUID interviewId);
}
