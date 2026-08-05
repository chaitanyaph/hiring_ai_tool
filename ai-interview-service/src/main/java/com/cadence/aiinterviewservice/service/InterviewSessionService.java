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

    /** jobId/candidateId are resolved server-side from the CandidateShortlist record for this application -- never trust caller-supplied IDs for who gets invited to what. */
    void inviteCandidate(UUID applicationId);

    void sendReminder(UUID applicationId);

    void resendInvite(UUID applicationId);

    InterviewQuestionResponse startInterview(UUID applicationId, UUID candidateId, InterviewMode mode);

    InterviewQuestionResponse submitAnswer(UUID applicationId, UUID candidateId, AnswerRequest request);

    void finishInterview(UUID applicationId, UUID candidateId);

    void recordRecruiterDecision(UUID applicationId, HiringRecommendation override);
}
