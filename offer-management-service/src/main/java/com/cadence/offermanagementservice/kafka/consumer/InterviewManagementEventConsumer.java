package com.cadence.offermanagementservice.kafka.consumer;

import com.cadence.offermanagementservice.constants.KafkaTopics;
import com.cadence.offermanagementservice.kafka.event.CandidateSelectedEvent;
import com.cadence.offermanagementservice.service.OfferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Wrapped in try/catch: a malformed event should never crash the consumer thread or block the partition, same defensive posture as every sibling service. */
@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewManagementEventConsumer {

    private final OfferService offerService;

    @KafkaListener(topics = KafkaTopics.CANDIDATE_SELECTED, groupId = "offer-management-service-group")
    public void onCandidateSelected(CandidateSelectedEvent event) {
        try {
            offerService.upsertDraftFromCandidateSelected(event.getApplicationId(), event.getCandidateId());
        } catch (Exception e) {
            log.error("Failed to process CandidateSelected for application {}: {}", event.getApplicationId(), e.getMessage(), e);
        }
    }
}
