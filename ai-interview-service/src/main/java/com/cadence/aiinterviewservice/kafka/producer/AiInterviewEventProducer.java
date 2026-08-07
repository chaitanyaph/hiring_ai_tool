package com.cadence.aiinterviewservice.kafka.producer;

import com.cadence.aiinterviewservice.constants.KafkaTopics;
import com.cadence.aiinterviewservice.kafka.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Every publish method is @Async: KafkaTemplate.send() blocks the
 * calling thread while resolving broker metadata *before* returning a
 * future, so a Kafka outage would otherwise stall the shortlisting/
 * interview/evaluation pipelines. Each entity row is always the
 * source of truth -- Kafka is best-effort fan-out on top of it.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiInterviewEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Async
    public void publishCandidateShortlisted(CandidateShortlistedEvent event) {
        publish(KafkaTopics.CANDIDATE_SHORTLISTED, event.getApplicationId().toString(), event);
    }

    @Async
    public void publishAiInterviewInvited(AiInterviewInvitedEvent event) {
        publish(KafkaTopics.INTERVIEW_INVITED, event.getApplicationId().toString(), event);
    }

    @Async
    public void publishInterviewStarted(InterviewStartedEvent event) {
        publish(KafkaTopics.INTERVIEW_STARTED, event.getApplicationId().toString(), event);
    }

    @Async
    public void publishInterviewCompleted(InterviewCompletedEvent event) {
        publish(KafkaTopics.INTERVIEW_COMPLETED, event.getApplicationId().toString(), event);
    }

    @Async
    public void publishInterviewEvaluated(InterviewEvaluatedEvent event) {
        publish(KafkaTopics.INTERVIEW_EVALUATED, event.getApplicationId().toString(), event);
    }

    @Async
    public void publishCandidateRecommended(CandidateRecommendedEvent event) {
        publish(KafkaTopics.CANDIDATE_RECOMMENDED, event.getApplicationId().toString(), event);
    }

    @Async
    public void publishInterviewReminder(AiInterviewReminderEvent event) {
        publish(KafkaTopics.INTERVIEW_REMINDER, event.getApplicationId().toString(), event);
    }

    @Async
    public void publishInterviewExpired(AiInterviewExpiredEvent event) {
        publish(KafkaTopics.INTERVIEW_EXPIRED, event.getApplicationId().toString(), event);
    }

    private void publish(String topic, String key, Object payload) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, payload);
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish event to topic [{}] key [{}]: {}", topic, key, ex.getMessage(), ex);
            } else {
                log.debug("Published event to topic [{}] partition [{}] offset [{}]",
                        topic, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            }
        });
    }
}
