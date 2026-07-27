package com.cadence.notificationservice.kafka.consumer;

import com.cadence.notificationservice.constants.KafkaTopics;
import com.cadence.notificationservice.kafka.event.ResumeAnalyzedEvent;
import com.cadence.notificationservice.kafka.event.ResumeUploadedEvent;
import com.cadence.notificationservice.service.NotificationOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeEventConsumer {

    private final NotificationOrchestrationService orchestrationService;

    @KafkaListener(topics = KafkaTopics.RESUME_UPLOADED, groupId = "notification-service-group")
    public void onResumeUploaded(ResumeUploadedEvent event) {
        try {
            orchestrationService.handleResumeUploaded(event);
        } catch (Exception e) {
            log.error("Failed to process ResumeUploaded for resume {}: {}", event.getResumeId(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaTopics.RESUME_ANALYZED, groupId = "notification-service-group")
    public void onResumeAnalyzed(ResumeAnalyzedEvent event) {
        try {
            orchestrationService.handleResumeAnalyzed(event);
        } catch (Exception e) {
            log.error("Failed to process ResumeAnalyzed for application {}: {}", event.getApplicationId(), e.getMessage(), e);
        }
    }
}
