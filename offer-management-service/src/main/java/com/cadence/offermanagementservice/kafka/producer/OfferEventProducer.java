package com.cadence.offermanagementservice.kafka.producer;

import com.cadence.offermanagementservice.constants.KafkaTopics;
import com.cadence.offermanagementservice.kafka.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Every publish method is @Async, same reasoning as every sibling
 * service: KafkaTemplate.send() blocks the calling thread resolving
 * broker metadata before returning a future, so a Kafka outage would
 * otherwise stall the offer workflow. publishApplicationOfferReleased/
 * Accepted/Rejected are the three bridges onto already-live consumers
 * (application-service's own status machine, and notification-
 * service's existing templates) -- the real integration value; every
 * other publish*() method is forward-scaffolded.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OfferEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Async
    public void publishOfferGenerated(OfferGeneratedEvent event) {
        publish(KafkaTopics.OFFER_GENERATED, event.getOfferId().toString(), event);
    }

    @Async
    public void publishOfferApproved(OfferApprovedEvent event) {
        publish(KafkaTopics.OFFER_APPROVED, event.getOfferId().toString(), event);
    }

    @Async
    public void publishOfferSent(OfferSentEvent event) {
        publish(KafkaTopics.OFFER_SENT, event.getOfferId().toString(), event);
    }

    @Async
    public void publishOfferAccepted(OfferAcceptedEvent event) {
        publish(KafkaTopics.OFFER_ACCEPTED, event.getOfferId().toString(), event);
    }

    @Async
    public void publishOfferRejected(OfferRejectedEvent event) {
        publish(KafkaTopics.OFFER_REJECTED, event.getOfferId().toString(), event);
    }

    @Async
    public void publishOfferNegotiationRequested(OfferNegotiationRequestedEvent event) {
        publish(KafkaTopics.OFFER_NEGOTIATION_REQUESTED, event.getOfferId().toString(), event);
    }

    @Async
    public void publishCandidateOnboardingStarted(CandidateOnboardingStartedEvent event) {
        publish(KafkaTopics.CANDIDATE_ONBOARDING_STARTED, event.getOfferId().toString(), event);
    }

    @Async
    public void publishApplicationOfferReleased(ApplicationOfferReleasedEvent event) {
        publish(KafkaTopics.APPLICATION_OFFER_RELEASED, event.getApplicationId().toString(), event);
    }

    @Async
    public void publishApplicationOfferAccepted(ApplicationOfferAcceptedEvent event) {
        publish(KafkaTopics.APPLICATION_OFFER_ACCEPTED, event.getApplicationId().toString(), event);
    }

    @Async
    public void publishApplicationOfferRejected(ApplicationOfferRejectedEvent event) {
        publish(KafkaTopics.APPLICATION_OFFER_REJECTED, event.getApplicationId().toString(), event);
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
