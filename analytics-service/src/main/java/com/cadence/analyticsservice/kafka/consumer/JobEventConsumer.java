package com.cadence.analyticsservice.kafka.consumer;

import com.cadence.analyticsservice.constants.KafkaTopics;
import com.cadence.analyticsservice.kafka.event.JobClosedEvent;
import com.cadence.analyticsservice.kafka.event.JobPublishedEvent;
import com.cadence.analyticsservice.service.MetricIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobEventConsumer {

    private final MetricIngestionService metricIngestionService;

    @KafkaListener(topics = KafkaTopics.JOB_PUBLISHED, groupId = "analytics-service-group")
    public void onJobPublished(JobPublishedEvent event) {
        try {
            metricIngestionService.onJobPublished(event);
        } catch (Exception e) {
            log.error("Failed to process JobPublished for job {}: {}", event.getJobId(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaTopics.JOB_CLOSED, groupId = "analytics-service-group")
    public void onJobClosed(JobClosedEvent event) {
        try {
            metricIngestionService.onJobClosed(event);
        } catch (Exception e) {
            log.error("Failed to process JobClosed for job {}: {}", event.getJobId(), e.getMessage(), e);
        }
    }
}
