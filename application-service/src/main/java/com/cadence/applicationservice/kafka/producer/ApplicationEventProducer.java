package com.cadence.applicationservice.kafka.producer;

import com.cadence.applicationservice.constant.KafkaTopics;
import com.cadence.applicationservice.kafka.event.*;
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
 * future, so a Kafka outage would otherwise stall every application
 * write. Failures are logged, never thrown -- the write to MySQL is
 * always the source of truth, Kafka is best-effort fan-out.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Async
    public void publishApplicationCreated(ApplicationCreatedEvent event) {
        publish(KafkaTopics.APPLICATION_CREATED, event.getApplicationId().toString(), event);
    }

    @Async
    public void publishApplicationWithdrawn(ApplicationWithdrawnEvent event) {
        publish(KafkaTopics.APPLICATION_WITHDRAWN, event.getApplicationId().toString(), event);
    }

    @Async
    public void publishApplicationStatusChanged(ApplicationStatusChangedEvent event) {
        publish(KafkaTopics.APPLICATION_STATUS_CHANGED, event.getApplicationId().toString(), event);
    }

    @Async
    public void publishRecruiterAssigned(RecruiterAssignedEvent event) {
        publish(KafkaTopics.RECRUITER_ASSIGNED, event.getApplicationId().toString(), event);
    }

    @Async
    public void publishHiringManagerAssigned(HiringManagerAssignedEvent event) {
        publish(KafkaTopics.HIRING_MANAGER_ASSIGNED, event.getApplicationId().toString(), event);
    }

    @Async
    public void publishOfferAccepted(OfferAcceptedEvent event) {
        publish(KafkaTopics.OFFER_ACCEPTED, event.getApplicationId().toString(), event);
    }

    @Async
    public void publishOfferRejected(OfferRejectedEvent event) {
        publish(KafkaTopics.OFFER_REJECTED, event.getApplicationId().toString(), event);
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
