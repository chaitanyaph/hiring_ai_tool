package com.cadence.notificationservice.kafka.consumer;

import com.cadence.notificationservice.constants.KafkaTopics;
import com.cadence.notificationservice.kafka.event.*;
import com.cadence.notificationservice.service.NotificationOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewManagementEventConsumer {

    private final NotificationOrchestrationService orchestrationService;

    @KafkaListener(topics = KafkaTopics.INTERVIEW_SCHEDULED, groupId = "notification-service-group")
    public void onInterviewScheduled(InterviewScheduledEvent event) {
        try {
            orchestrationService.handleInterviewScheduled(event);
        } catch (Exception e) {
            log.error("Failed to process InterviewScheduled for interview {}: {}", event.getInterviewId(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaTopics.INTERVIEW_RESCHEDULED, groupId = "notification-service-group")
    public void onInterviewRescheduled(InterviewRescheduledEvent event) {
        try {
            orchestrationService.handleInterviewRescheduled(event);
        } catch (Exception e) {
            log.error("Failed to process InterviewRescheduled for interview {}: {}", event.getInterviewId(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaTopics.INTERVIEW_CANCELLED, groupId = "notification-service-group")
    public void onInterviewCancelled(InterviewCancelledEvent event) {
        try {
            orchestrationService.handleInterviewCancelled(event);
        } catch (Exception e) {
            log.error("Failed to process InterviewCancelled for interview {}: {}", event.getInterviewId(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaTopics.CANDIDATE_MOVED_TO_HR, groupId = "notification-service-group")
    public void onCandidateMovedToHr(CandidateMovedToHrEvent event) {
        try {
            orchestrationService.handleCandidateMovedToHr(event);
        } catch (Exception e) {
            log.error("Failed to process CandidateMovedToHr for application {}: {}", event.getApplicationId(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaTopics.CANDIDATE_SELECTED, groupId = "notification-service-group")
    public void onCandidateSelected(CandidateSelectedEvent event) {
        try {
            orchestrationService.handleCandidateSelected(event);
        } catch (Exception e) {
            log.error("Failed to process CandidateSelected for application {}: {}", event.getApplicationId(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaTopics.CANDIDATE_REJECTED, groupId = "notification-service-group")
    public void onCandidateRejected(CandidateRejectedEvent event) {
        try {
            orchestrationService.handleCandidateRejected(event);
        } catch (Exception e) {
            log.error("Failed to process CandidateRejected for application {}: {}", event.getApplicationId(), e.getMessage(), e);
        }
    }
}
