package com.cadence.notificationservice.kafka.consumer;

import com.cadence.notificationservice.constants.KafkaTopics;
import com.cadence.notificationservice.kafka.event.JobClosedEvent;
import com.cadence.notificationservice.kafka.event.JobPublishedEvent;
import com.cadence.notificationservice.service.NotificationOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobEventConsumer {

    private final NotificationOrchestrationService orchestrationService;

    @KafkaListener(topics = KafkaTopics.JOB_PUBLISHED, groupId = "notification-service-group")
    public void onJobPublished(JobPublishedEvent event) {
        try {
            orchestrationService.handleJobPublished(event);
        } catch (Exception e) {
            log.error("Failed to process JobPublished for job {}: {}", event.getJobId(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaTopics.JOB_CLOSED, groupId = "notification-service-group")
    public void onJobClosed(JobClosedEvent event) {
        try {
            orchestrationService.handleJobClosed(event);
        } catch (Exception e) {
            log.error("Failed to process JobClosed for job {}: {}", event.getJobId(), e.getMessage(), e);
        }
    }
}
