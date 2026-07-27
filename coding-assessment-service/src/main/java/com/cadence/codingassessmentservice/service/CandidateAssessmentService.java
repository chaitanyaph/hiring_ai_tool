package com.cadence.codingassessmentservice.service;

import com.cadence.codingassessmentservice.constants.AntiCheatEventType;
import com.cadence.codingassessmentservice.constants.ProgrammingLanguage;
import com.cadence.codingassessmentservice.dto.response.IdeQuestionResponse;

import java.util.UUID;

/** Orchestration for a candidate's own attempt: invitation, rules acceptance, the turn-based start/navigate/finish flow, and anti-cheat event capture. */
public interface CandidateAssessmentService {

    void inviteCandidate(UUID assessmentId, UUID applicationId, UUID jobId, UUID candidateId);

    void sendReminder(UUID candidateAssessmentId);

    void resendInvite(UUID candidateAssessmentId);

    void acceptRules(UUID candidateAssessmentId);

    IdeQuestionResponse startAssessment(UUID candidateAssessmentId, ProgrammingLanguage preferredLanguage);

    IdeQuestionResponse goToQuestion(UUID candidateAssessmentId, int questionIndex);

    void markForReview(UUID candidateAssessmentId, UUID questionId, boolean marked);

    void finishAssessment(UUID candidateAssessmentId, boolean autoSubmitted);

    void recordAntiCheatEvent(UUID candidateAssessmentId, AntiCheatEventType eventType, String metadata);
}
