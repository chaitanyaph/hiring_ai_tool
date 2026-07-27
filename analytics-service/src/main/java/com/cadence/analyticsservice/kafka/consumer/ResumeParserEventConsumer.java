package com.cadence.analyticsservice.kafka.consumer;

import com.cadence.analyticsservice.constants.KafkaTopics;
import com.cadence.analyticsservice.kafka.event.ResumeAnalyzedEvent;
import com.cadence.analyticsservice.kafka.event.ResumeParsedEvent;
import com.cadence.analyticsservice.service.MetricIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeParserEventConsumer {

    private final MetricIngestionService metricIngestionService;

    @KafkaListener(topics = KafkaTopics.RESUME_PARSED, groupId = "analytics-service-group")
    public void onResumeParsed(ResumeParsedEvent event) {
        try {
            metricIngestionService.onResumeParsed(event);
        } catch (Exception e) {
            log.error("Failed to process ResumeParsed for resume {}: {}", event.getResumeId(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaTopics.RESUME_ANALYZED, groupId = "analytics-service-group")
    public void onResumeAnalyzed(ResumeAnalyzedEvent event) {
        try {
            metricIngestionService.onResumeAnalyzed(event);
        } catch (Exception e) {
            log.error("Failed to process ResumeAnalyzed for application {}: {}", event.getApplicationId(), e.getMessage(), e);
        }
    }
}
