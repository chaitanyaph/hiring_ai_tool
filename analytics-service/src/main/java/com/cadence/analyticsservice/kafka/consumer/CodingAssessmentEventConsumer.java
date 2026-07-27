package com.cadence.analyticsservice.kafka.consumer;

import com.cadence.analyticsservice.constants.KafkaTopics;
import com.cadence.analyticsservice.kafka.event.CodingAssessmentCompletedEvent;
import com.cadence.analyticsservice.service.MetricIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CodingAssessmentEventConsumer {

    private final MetricIngestionService metricIngestionService;

    @KafkaListener(topics = KafkaTopics.CODING_ASSESSMENT_COMPLETED, groupId = "analytics-service-group")
    public void onCodingAssessmentCompleted(CodingAssessmentCompletedEvent event) {
        try {
            metricIngestionService.onCodingAssessmentCompleted(event);
        } catch (Exception e) {
            log.error("Failed to process CodingAssessmentCompleted for application {}: {}", event.getApplicationId(), e.getMessage(), e);
        }
    }
}
