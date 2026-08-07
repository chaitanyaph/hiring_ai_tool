package com.cadence.notificationservice.kafka.consumer;

import com.cadence.notificationservice.constants.KafkaTopics;
import com.cadence.notificationservice.kafka.event.AiInterviewCompletedEvent;
import com.cadence.notificationservice.kafka.event.AiInterviewExpiredEvent;
import com.cadence.notificationservice.kafka.event.AiInterviewInvitedEvent;
import com.cadence.notificationservice.kafka.event.AiInterviewReminderEvent;
import com.cadence.notificationservice.kafka.event.CandidateShortlistedEvent;
import com.cadence.notificationservice.kafka.event.InterviewEvaluatedEvent;
import com.cadence.notificationservice.service.NotificationOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiInterviewEventConsumer {

    private final NotificationOrchestrationService orchestrationService;

    @KafkaListener(topics = KafkaTopics.CANDIDATE_SHORTLISTED, groupId = "notification-service-group")
    public void onCandidateShortlisted(CandidateShortlistedEvent event) {
        try {
            orchestrationService.handleCandidateShortlisted(event);
        } catch (Exception e) {
            log.error("Failed to process CandidateShortlisted for application {}: {}", event.getApplicationId(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaTopics.AI_INTERVIEW_INVITED, groupId = "notification-service-group")
    public void onAiInterviewInvited(AiInterviewInvitedEvent event) {
        try {
            orchestrationService.handleAiInterviewInvited(event);
        } catch (Exception e) {
            log.error("Failed to process AiInterviewInvited for application {}: {}", event.getApplicationId(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaTopics.AI_INTERVIEW_COMPLETED, groupId = "notification-service-group")
    public void onAiInterviewCompleted(AiInterviewCompletedEvent event) {
        try {
            orchestrationService.handleAiInterviewCompleted(event);
        } catch (Exception e) {
            log.error("Failed to process AIInterviewCompleted for application {}: {}", event.getApplicationId(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaTopics.AI_INTERVIEW_EVALUATED, groupId = "notification-service-group")
    public void onAiInterviewEvaluated(InterviewEvaluatedEvent event) {
        try {
            orchestrationService.handleAiInterviewEvaluated(event);
        } catch (Exception e) {
            log.error("Failed to process AiInterviewEvaluated for application {}: {}", event.getApplicationId(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaTopics.AI_INTERVIEW_EXPIRED, groupId = "notification-service-group")
    public void onAiInterviewExpired(AiInterviewExpiredEvent event) {
        try {
            orchestrationService.handleAiInterviewExpired(event);
        } catch (Exception e) {
            log.error("Failed to process AiInterviewExpired for application {}: {}", event.getApplicationId(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaTopics.AI_INTERVIEW_REMINDER, groupId = "notification-service-group")
    public void onAiInterviewReminder(AiInterviewReminderEvent event) {
        try {
            orchestrationService.handleAiInterviewReminder(event);
        } catch (Exception e) {
            log.error("Failed to process AiInterviewReminder for application {}: {}", event.getApplicationId(), e.getMessage(), e);
        }
    }
}
