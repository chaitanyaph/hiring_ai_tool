package com.cadence.resumeservice.kafka.producer;

import com.cadence.resumeservice.constants.KafkaTopics;
import com.cadence.resumeservice.event.ResumeDefaultChangedEvent;
import com.cadence.resumeservice.event.ResumeDeletedEvent;
import com.cadence.resumeservice.event.ResumeUploadedEvent;
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
 * future, so a Kafka outage would otherwise stall every upload/delete.
 * The MinIO write and MySQL row are always the source of truth --
 * Kafka is best-effort fan-out on top of them.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Async
    public void publishResumeUploaded(ResumeUploadedEvent event) {
        publish(KafkaTopics.RESUME_UPLOADED, event.getResumeId().toString(), event);
    }

    @Async
    public void publishResumeDeleted(ResumeDeletedEvent event) {
        publish(KafkaTopics.RESUME_DELETED, event.getResumeId().toString(), event);
    }

    @Async
    public void publishResumeDefaultChanged(ResumeDefaultChangedEvent event) {
        publish(KafkaTopics.RESUME_DEFAULT_CHANGED, event.getCandidateId().toString(), event);
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
