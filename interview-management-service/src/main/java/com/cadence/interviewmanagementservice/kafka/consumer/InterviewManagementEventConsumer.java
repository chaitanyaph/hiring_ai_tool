package com.cadence.interviewmanagementservice.kafka.consumer;

import com.cadence.interviewmanagementservice.constants.KafkaTopics;
import com.cadence.interviewmanagementservice.kafka.event.CandidateRecommendedEvent;
import com.cadence.interviewmanagementservice.kafka.event.CodingAssessmentCompletedEvent;
import com.cadence.interviewmanagementservice.service.CandidateTimelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Wrapped in try/catch: a malformed event should never crash the consumer thread or block the partition, same defensive posture every sibling service already takes. */
@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewManagementEventConsumer {

    private final CandidateTimelineService candidateTimelineService;

    @KafkaListener(topics = KafkaTopics.CANDIDATE_RECOMMENDED, groupId = "interview-management-service-group")
    public void onCandidateRecommended(CandidateRecommendedEvent event) {
        try {
            candidateTimelineService.handleCandidateRecommended(event);
        } catch (Exception e) {
            log.error("Failed to process CandidateRecommended for application {}: {}", event.getApplicationId(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaTopics.CODING_ASSESSMENT_COMPLETED, groupId = "interview-management-service-group")
    public void onCodingAssessmentCompleted(CodingAssessmentCompletedEvent event) {
        try {
            candidateTimelineService.handleCodingAssessmentCompleted(event);
        } catch (Exception e) {
            log.error("Failed to process CodingAssessmentCompleted for application {}: {}", event.getApplicationId(), e.getMessage(), e);
        }
    }
}
