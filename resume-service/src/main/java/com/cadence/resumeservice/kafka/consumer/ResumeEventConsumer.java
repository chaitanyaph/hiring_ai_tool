package com.cadence.resumeservice.kafka.consumer;

import com.cadence.resumeservice.constants.KafkaTopics;
import com.cadence.resumeservice.event.CandidateDeletedEvent;
import com.cadence.resumeservice.service.ResumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Reacts to a candidate account being deleted by archiving every
 * resume they ever uploaded (not hard-deleting the MinIO objects
 * outright -- see ResumeServiceImpl.handleCandidateDeleted for why).
 * Wrapped in try/catch: a malformed event should never crash the
 * consumer thread or block the partition.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeEventConsumer {

    private final ResumeService resumeService;

    @KafkaListener(topics = KafkaTopics.CANDIDATE_DELETED, groupId = "resume-service-group")
    public void onCandidateDeleted(CandidateDeletedEvent event) {
        try {
            resumeService.handleCandidateDeleted(event.getCandidateId());
        } catch (Exception e) {
            log.error("Failed to process CandidateDeleted for candidate {}: {}", event.getCandidateId(), e.getMessage(), e);
        }
    }
}
