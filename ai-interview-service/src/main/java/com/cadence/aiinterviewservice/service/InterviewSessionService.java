package com.cadence.aiinterviewservice.service;

import com.cadence.aiinterviewservice.constants.HiringRecommendation;
import com.cadence.aiinterviewservice.constants.InterviewMode;
import com.cadence.aiinterviewservice.dto.request.AnswerRequest;
import com.cadence.aiinterviewservice.dto.response.InterviewQuestionResponse;

import java.util.UUID;

/**
 * Orchestration / write side of the AI Interview module: recruiter
 * invitation actions, the candidate's turn-based start/answer/finish
 * flow, and the recruiter's post-report decision actions.
 */
public interface InterviewSessionService {

    void inviteCandidate(UUID applicationId, UUID jobId, UUID candidateId);

    void sendReminder(UUID applicationId);

    void resendInvite(UUID applicationId);

    InterviewQuestionResponse startInterview(UUID applicationId, InterviewMode mode);

    InterviewQuestionResponse submitAnswer(UUID applicationId, AnswerRequest request);

    void finishInterview(UUID applicationId);

    void recordRecruiterDecision(UUID applicationId, HiringRecommendation override);
}
