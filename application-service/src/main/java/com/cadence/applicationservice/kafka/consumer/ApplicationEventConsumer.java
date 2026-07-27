package com.cadence.applicationservice.kafka.consumer;

import com.cadence.applicationservice.constant.KafkaTopics;
import com.cadence.applicationservice.kafka.event.*;
import com.cadence.applicationservice.service.ApplicationLifecycleEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Reacts to the 7 events published by other (future) services in the
 * hiring pipeline. Every handler is wrapped in try/catch: a malformed
 * or out-of-order event should never crash the consumer thread or
 * block the partition -- ApplicationLifecycleEventService already
 * no-ops with a warning log if the application is missing or in an
 * unexpected status, so these catches are for anything unexpected
 * beyond that (e.g. a transient DB error).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationEventConsumer {

    private final ApplicationLifecycleEventService lifecycleEventService;

    @KafkaListener(topics = KafkaTopics.RESUME_PARSED, groupId = "application-service-group")
    public void onResumeParsed(ResumeParsedEvent event) {
        try {
            lifecycleEventService.handleResumeParsed(event.getApplicationId(), event.getResumeId());
        } catch (Exception e) {
            log.error("Failed to process ResumeParsed for application {}: {}", event.getApplicationId(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaTopics.RESUME_MATCHED, groupId = "application-service-group")
    public void onResumeMatched(ResumeMatchedEvent event) {
        try {
            lifecycleEventService.handleResumeMatched(event.getApplicationId(), event.getMatchScore());
        } catch (Exception e) {
            log.error("Failed to process ResumeMatched for application {}: {}", event.getApplicationId(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaTopics.CANDIDATE_SHORTLISTED, groupId = "application-service-group")
    public void onCandidateShortlisted(CandidateShortlistedEvent event) {
        try {
            lifecycleEventService.handleCandidateShortlisted(event.getApplicationId());
        } catch (Exception e) {
            log.error("Failed to process CandidateShortlisted for application {}: {}", event.getApplicationId(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaTopics.INTERVIEW_COMPLETED, groupId = "application-service-group")
    public void onInterviewCompleted(InterviewCompletedEvent event) {
        try {
            lifecycleEventService.handleInterviewCompleted(event.getApplicationId(), event.getInterviewType(), event.getScore(), event.getFeedback());
        } catch (Exception e) {
            log.error("Failed to process InterviewCompleted for application {}: {}", event.getApplicationId(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaTopics.CODING_ASSESSMENT_COMPLETED, groupId = "application-service-group")
    public void onCodingAssessmentCompleted(CodingAssessmentCompletedEvent event) {
        try {
            lifecycleEventService.handleCodingAssessmentCompleted(event.getApplicationId(), event.getScore());
        } catch (Exception e) {
            log.error("Failed to process CodingAssessmentCompleted for application {}: {}", event.getApplicationId(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaTopics.BACKGROUND_VERIFICATION_COMPLETED, groupId = "application-service-group")
    public void onBackgroundVerificationCompleted(BackgroundVerificationCompletedEvent event) {
        try {
            lifecycleEventService.handleBackgroundVerificationCompleted(event.getApplicationId(), event.isPassed(), event.getRemarks());
        } catch (Exception e) {
            log.error("Failed to process BackgroundVerificationCompleted for application {}: {}", event.getApplicationId(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaTopics.OFFER_RELEASED, groupId = "application-service-group")
    public void onOfferReleased(OfferReleasedEvent event) {
        try {
            lifecycleEventService.handleOfferReleased(event.getApplicationId(), event.getOfferId());
        } catch (Exception e) {
            log.error("Failed to process OfferReleased for application {}: {}", event.getApplicationId(), e.getMessage(), e);
        }
    }
}
