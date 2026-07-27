package com.cadence.jobservice.kafka.producer;

import com.cadence.jobservice.constant.KafkaTopics;
import com.cadence.jobservice.kafka.event.*;
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
 * future, so a Kafka outage would otherwise stall every job write.
 * Failures are logged, never thrown.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Async
    public void publishJobCreated(JobCreatedEvent event) {
        publish(KafkaTopics.JOB_CREATED, event.getJobId().toString(), event);
    }

    @Async
    public void publishJobUpdated(JobUpdatedEvent event) {
        publish(KafkaTopics.JOB_UPDATED, event.getJobId().toString(), event);
    }

    @Async
    public void publishJobPublished(JobPublishedEvent event) {
        publish(KafkaTopics.JOB_PUBLISHED, event.getJobId().toString(), event);
    }

    @Async
    public void publishJobArchived(JobArchivedEvent event) {
        publish(KafkaTopics.JOB_ARCHIVED, event.getJobId().toString(), event);
    }

    @Async
    public void publishJobClosed(JobClosedEvent event) {
        publish(KafkaTopics.JOB_CLOSED, event.getJobId().toString(), event);
    }

    @Async
    public void publishJobRestored(JobRestoredEvent event) {
        publish(KafkaTopics.JOB_RESTORED, event.getJobId().toString(), event);
    }

    @Async
    public void publishJobDeleted(JobDeletedEvent event) {
        publish(KafkaTopics.JOB_DELETED, event.getJobId().toString(), event);
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
