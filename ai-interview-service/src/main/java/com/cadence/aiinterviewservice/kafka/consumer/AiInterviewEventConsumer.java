package com.cadence.aiinterviewservice.kafka.consumer;

import com.cadence.aiinterviewservice.constants.KafkaTopics;
import com.cadence.aiinterviewservice.constants.ShortlistDecision;
import com.cadence.aiinterviewservice.kafka.event.CandidateShortlistedEvent;
import com.cadence.aiinterviewservice.kafka.event.ResumeAnalyzedEvent;
import com.cadence.aiinterviewservice.service.InterviewSessionService;
import com.cadence.aiinterviewservice.service.ShortlistingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Wrapped in try/catch: a malformed event should never crash the consumer thread or block the partition, same defensive posture every sibling service already takes. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiInterviewEventConsumer {

    private final ShortlistingService shortlistingService;
    private final InterviewSessionService interviewSessionService;

    @KafkaListener(topics = KafkaTopics.RESUME_ANALYZED, groupId = "ai-interview-service-group")
    public void onResumeAnalyzed(ResumeAnalyzedEvent event) {
        try {
            shortlistingService.handleResumeAnalyzed(event);
        } catch (Exception e) {
            log.error("Failed to process ResumeAnalyzed for application {}: {}", event.getApplicationId(), e.getMessage(), e);
        }
    }

    /**
     * This service publishes CANDIDATE_SHORTLISTED itself (from
     * ShortlistingServiceImpl) but never used to consume its own topic --
     * the interview session was only ever created by a recruiter manually
     * clicking "Start" in the AI Interviews queue. Auto-inviting here closes
     * that gap for the SHORTLISTED decision; REJECTED/MANUAL_REVIEW don't
     * get an interview at all (MANUAL_REVIEW gets one later if a recruiter
     * resolves it to SHORTLISTED, which republishes this same event).
     */
    @KafkaListener(topics = KafkaTopics.CANDIDATE_SHORTLISTED, groupId = "ai-interview-service-group")
    public void onCandidateShortlisted(CandidateShortlistedEvent event) {
        if (event.getDecision() != ShortlistDecision.SHORTLISTED) {
            return;
        }
        try {
            interviewSessionService.inviteCandidate(event.getApplicationId());
        } catch (Exception e) {
            log.error("Failed to auto-invite application {} to AI interview: {}", event.getApplicationId(), e.getMessage(), e);
        }
    }
}
