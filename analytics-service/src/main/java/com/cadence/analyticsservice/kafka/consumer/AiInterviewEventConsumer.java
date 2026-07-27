package com.cadence.analyticsservice.kafka.consumer;

import com.cadence.analyticsservice.constants.KafkaTopics;
import com.cadence.analyticsservice.kafka.event.CandidateShortlistedEvent;
import com.cadence.analyticsservice.kafka.event.InterviewEvaluatedEvent;
import com.cadence.analyticsservice.service.MetricIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiInterviewEventConsumer {

    private final MetricIngestionService metricIngestionService;

    @KafkaListener(topics = KafkaTopics.CANDIDATE_SHORTLISTED, groupId = "analytics-service-group")
    public void onCandidateShortlisted(CandidateShortlistedEvent event) {
        try {
            metricIngestionService.onCandidateShortlisted(event);
        } catch (Exception e) {
            log.error("Failed to process CandidateShortlisted for application {}: {}", event.getApplicationId(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaTopics.INTERVIEW_EVALUATED, groupId = "analytics-service-group")
    public void onInterviewEvaluated(InterviewEvaluatedEvent event) {
        try {
            metricIngestionService.onInterviewEvaluated(event);
        } catch (Exception e) {
            log.error("Failed to process InterviewEvaluated for application {}: {}", event.getApplicationId(), e.getMessage(), e);
        }
    }
}
