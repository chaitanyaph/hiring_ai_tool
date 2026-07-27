package com.cadence.candidateservice.kafka.producer;

import com.cadence.candidateservice.constant.KafkaTopics;
import com.cadence.candidateservice.kafka.event.*;
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
 * future, so a Kafka outage would otherwise stall every profile/
 * application write. Failures are logged, never thrown.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CandidateEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Async
    public void publishProfileCreated(ProfileCreatedEvent event) {
        publish(KafkaTopics.PROFILE_CREATED, event.getCandidateId().toString(), event);
    }

    @Async
    public void publishProfileUpdated(ProfileUpdatedEvent event) {
        publish(KafkaTopics.PROFILE_UPDATED, event.getCandidateId().toString(), event);
    }

    @Async
    public void publishResumeUploaded(ResumeUploadedEvent event) {
        publish(KafkaTopics.RESUME_UPLOADED, event.getCandidateId().toString(), event);
    }

    @Async
    public void publishApplicationSubmitted(ApplicationSubmittedEvent event) {
        publish(KafkaTopics.APPLICATION_SUBMITTED, event.getApplicationId().toString(), event);
    }

    @Async
    public void publishApplicationWithdrawn(ApplicationWithdrawnEvent event) {
        publish(KafkaTopics.APPLICATION_WITHDRAWN, event.getApplicationId().toString(), event);
    }

    @Async
    public void publishApplicationStageChanged(ApplicationStageChangedEvent event) {
        publish(KafkaTopics.APPLICATION_STAGE_CHANGED, event.getApplicationId().toString(), event);
    }

    @Async
    public void publishJobSaved(JobSavedEvent event) {
        publish(KafkaTopics.JOB_SAVED, event.getCandidateId().toString(), event);
    }

    @Async
    public void publishJobUnsaved(JobUnsavedEvent event) {
        publish(KafkaTopics.JOB_UNSAVED, event.getCandidateId().toString(), event);
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
