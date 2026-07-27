package com.cadence.analyticsservice.kafka.consumer;

import com.cadence.analyticsservice.constants.KafkaTopics;
import com.cadence.analyticsservice.kafka.event.ApplicationCreatedEvent;
import com.cadence.analyticsservice.kafka.event.ApplicationStatusChangedEvent;
import com.cadence.analyticsservice.kafka.event.RecruiterAssignedEvent;
import com.cadence.analyticsservice.service.MetricIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationEventConsumer {

    private final MetricIngestionService metricIngestionService;

    @KafkaListener(topics = KafkaTopics.APPLICATION_CREATED, groupId = "analytics-service-group")
    public void onApplicationCreated(ApplicationCreatedEvent event) {
        try {
            metricIngestionService.onApplicationCreated(event);
        } catch (Exception e) {
            log.error("Failed to process ApplicationCreated for application {}: {}", event.getApplicationId(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaTopics.APPLICATION_STATUS_CHANGED, groupId = "analytics-service-group")
    public void onApplicationStatusChanged(ApplicationStatusChangedEvent event) {
        try {
            metricIngestionService.onApplicationStatusChanged(event);
        } catch (Exception e) {
            log.error("Failed to process ApplicationStatusChanged for application {}: {}", event.getApplicationId(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaTopics.RECRUITER_ASSIGNED, groupId = "analytics-service-group")
    public void onRecruiterAssigned(RecruiterAssignedEvent event) {
        try {
            metricIngestionService.onRecruiterAssigned(event);
        } catch (Exception e) {
            log.error("Failed to process RecruiterAssigned for application {}: {}", event.getApplicationId(), e.getMessage(), e);
        }
    }
}
