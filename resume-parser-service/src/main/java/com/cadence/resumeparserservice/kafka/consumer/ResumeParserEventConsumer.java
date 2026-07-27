package com.cadence.resumeparserservice.kafka.consumer;

import com.cadence.resumeparserservice.constants.KafkaTopics;
import com.cadence.resumeparserservice.kafka.event.ApplicationCreatedEvent;
import com.cadence.resumeparserservice.kafka.event.CandidateDeletedEvent;
import com.cadence.resumeparserservice.kafka.event.ResumeUploadedEvent;
import com.cadence.resumeparserservice.service.ResumeMatchAnalysisService;
import com.cadence.resumeparserservice.service.ResumeParsingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Every listener is wrapped in try/catch: a malformed event should
 * never crash the consumer thread or block the partition, same
 * defensive posture every sibling service already takes on its own
 * consumed events.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeParserEventConsumer {

    private final ResumeParsingService resumeParsingService;
    private final ResumeMatchAnalysisService resumeMatchAnalysisService;

    @KafkaListener(topics = KafkaTopics.RESUME_UPLOADED, groupId = "resume-parser-service-group")
    public void onResumeUploaded(ResumeUploadedEvent event) {
        try {
            resumeParsingService.processResume(event.getResumeId(), event.getCandidateId(), event.getChecksum());
        } catch (Exception e) {
            log.error("Failed to process ResumeUploaded for resume {}: {}", event.getResumeId(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaTopics.CANDIDATE_DELETED, groupId = "resume-parser-service-group")
    public void onCandidateDeleted(CandidateDeletedEvent event) {
        try {
            resumeParsingService.handleCandidateDeleted(event.getCandidateId());
        } catch (Exception e) {
            log.error("Failed to process CandidateDeleted for candidate {}: {}", event.getCandidateId(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaTopics.APPLICATION_CREATED, groupId = "resume-parser-service-group")
    public void onApplicationCreated(ApplicationCreatedEvent event) {
        try {
            resumeMatchAnalysisService.handleApplicationCreated(event);
        } catch (Exception e) {
            log.error("Failed to process ApplicationCreated for application {}: {}", event.getApplicationId(), e.getMessage(), e);
        }
    }
}
