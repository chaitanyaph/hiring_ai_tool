package com.cadence.codingassessmentservice.kafka.consumer;

import com.cadence.codingassessmentservice.constants.KafkaTopics;
import com.cadence.codingassessmentservice.kafka.event.CandidateRecommendedEvent;
import com.cadence.codingassessmentservice.service.AssessmentEligibilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Wrapped in try/catch: a malformed event should never crash the consumer thread or block the partition, same defensive posture every sibling service already takes. */
@Slf4j
@Component
@RequiredArgsConstructor
public class CodingAssessmentEventConsumer {

    private final AssessmentEligibilityService assessmentEligibilityService;

    @KafkaListener(topics = KafkaTopics.CANDIDATE_RECOMMENDED, groupId = "coding-assessment-service-group")
    public void onCandidateRecommended(CandidateRecommendedEvent event) {
        try {
            assessmentEligibilityService.handleCandidateRecommended(event);
        } catch (Exception e) {
            log.error("Failed to process CandidateRecommended for application {}: {}", event.getApplicationId(), e.getMessage(), e);
        }
    }
}
