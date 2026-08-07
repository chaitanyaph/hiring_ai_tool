package com.cadence.codingassessmentservice.kafka.producer;

import com.cadence.codingassessmentservice.constants.KafkaTopics;
import com.cadence.codingassessmentservice.kafka.event.*;
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
 * future, so a Kafka outage would otherwise stall the assessment/
 * submission/evaluation pipelines. Each entity row is always the
 * source of truth -- Kafka is best-effort fan-out on top of it.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CodingAssessmentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Async
    public void publishAssessmentCreated(AssessmentCreatedEvent event) {
        publish(KafkaTopics.ASSESSMENT_CREATED, event.getAssessmentId().toString(), event);
    }

    @Async
    public void publishAssessmentStarted(AssessmentStartedEvent event) {
        publish(KafkaTopics.ASSESSMENT_STARTED, event.getApplicationId().toString(), event);
    }

    @Async
    public void publishCodeSubmitted(CodeSubmittedEvent event) {
        publish(KafkaTopics.CODE_SUBMITTED, event.getApplicationId().toString(), event);
    }

    @Async
    public void publishCodingAssessmentCompleted(CodingAssessmentCompletedEvent event) {
        publish(KafkaTopics.CODING_ASSESSMENT_COMPLETED, event.getApplicationId().toString(), event);
    }

    @Async
    public void publishCodingAssessmentInvited(CodingAssessmentInvitedEvent event) {
        publish(KafkaTopics.CODING_ASSESSMENT_INVITED, event.getApplicationId().toString(), event);
    }

    @Async
    public void publishCodingAssessmentReminder(CodingAssessmentReminderEvent event) {
        publish(KafkaTopics.CODING_ASSESSMENT_REMINDER, event.getApplicationId().toString(), event);
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
