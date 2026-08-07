package com.cadence.notificationservice.kafka.consumer;

import com.cadence.notificationservice.constants.KafkaTopics;
import com.cadence.notificationservice.kafka.event.CodingAssessmentCompletedEvent;
import com.cadence.notificationservice.kafka.event.CodingAssessmentInvitedEvent;
import com.cadence.notificationservice.kafka.event.CodingAssessmentReminderEvent;
import com.cadence.notificationservice.service.NotificationOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CodingAssessmentEventConsumer {

    private final NotificationOrchestrationService orchestrationService;

    @KafkaListener(topics = KafkaTopics.CODING_ASSESSMENT_INVITED, groupId = "notification-service-group")
    public void onCodingAssessmentInvited(CodingAssessmentInvitedEvent event) {
        try {
            orchestrationService.handleCodingAssessmentInvited(event);
        } catch (Exception e) {
            log.error("Failed to process CodingAssessmentInvited for application {}: {}", event.getApplicationId(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaTopics.CODING_ASSESSMENT_COMPLETED, groupId = "notification-service-group")
    public void onCodingAssessmentCompleted(CodingAssessmentCompletedEvent event) {
        try {
            orchestrationService.handleCodingAssessmentCompleted(event);
        } catch (Exception e) {
            log.error("Failed to process CodingAssessmentCompleted for application {}: {}", event.getApplicationId(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaTopics.CODING_ASSESSMENT_REMINDER, groupId = "notification-service-group")
    public void onCodingAssessmentReminder(CodingAssessmentReminderEvent event) {
        try {
            orchestrationService.handleCodingAssessmentReminder(event);
        } catch (Exception e) {
            log.error("Failed to process CodingAssessmentReminder for application {}: {}", event.getApplicationId(), e.getMessage(), e);
        }
    }
}
