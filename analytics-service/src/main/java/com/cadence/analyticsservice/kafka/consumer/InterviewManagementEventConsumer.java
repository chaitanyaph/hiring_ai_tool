package com.cadence.analyticsservice.kafka.consumer;

import com.cadence.analyticsservice.constants.KafkaTopics;
import com.cadence.analyticsservice.kafka.event.InterviewManagementInterviewCancelledEvent;
import com.cadence.analyticsservice.kafka.event.InterviewManagementInterviewCompletedEvent;
import com.cadence.analyticsservice.service.MetricIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewManagementEventConsumer {

    private final MetricIngestionService metricIngestionService;

    @KafkaListener(topics = KafkaTopics.INTERVIEW_MANAGEMENT_INTERVIEW_COMPLETED, groupId = "analytics-service-group")
    public void onInterviewCompleted(InterviewManagementInterviewCompletedEvent event) {
        try {
            metricIngestionService.onInterviewCompleted(event);
        } catch (Exception e) {
            log.error("Failed to process InterviewCompleted for interview {}: {}", event.getInterviewId(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaTopics.INTERVIEW_MANAGEMENT_INTERVIEW_CANCELLED, groupId = "analytics-service-group")
    public void onInterviewCancelled(InterviewManagementInterviewCancelledEvent event) {
        try {
            metricIngestionService.onInterviewCancelled(event);
        } catch (Exception e) {
            log.error("Failed to process InterviewCancelled for interview {}: {}", event.getInterviewId(), e.getMessage(), e);
        }
    }
}
