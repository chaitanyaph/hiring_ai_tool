package com.cadence.aiinterviewservice.kafka.consumer;

import com.cadence.aiinterviewservice.constants.KafkaTopics;
import com.cadence.aiinterviewservice.kafka.event.ResumeAnalyzedEvent;
import com.cadence.aiinterviewservice.service.ShortlistingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Wrapped in try/catch: a malformed event should never crash the consumer thread or block the partition, same defensive posture every sibling service already takes. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiInterviewEventConsumer {

    private final ShortlistingService shortlistingService;

    @KafkaListener(topics = KafkaTopics.RESUME_ANALYZED, groupId = "ai-interview-service-group")
    public void onResumeAnalyzed(ResumeAnalyzedEvent event) {
        try {
            shortlistingService.handleResumeAnalyzed(event);
        } catch (Exception e) {
            log.error("Failed to process ResumeAnalyzed for application {}: {}", event.getApplicationId(), e.getMessage(), e);
        }
    }
}
