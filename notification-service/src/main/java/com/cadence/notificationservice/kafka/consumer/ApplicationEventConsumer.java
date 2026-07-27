package com.cadence.notificationservice.kafka.consumer;

import com.cadence.notificationservice.constants.KafkaTopics;
import com.cadence.notificationservice.kafka.event.ApplicationCreatedEvent;
import com.cadence.notificationservice.kafka.event.OfferAcceptedEvent;
import com.cadence.notificationservice.kafka.event.OfferRejectedEvent;
import com.cadence.notificationservice.service.NotificationOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationEventConsumer {

    private final NotificationOrchestrationService orchestrationService;

    @KafkaListener(topics = KafkaTopics.APPLICATION_CREATED, groupId = "notification-service-group")
    public void onApplicationCreated(ApplicationCreatedEvent event) {
        try {
            orchestrationService.handleApplicationCreated(event);
        } catch (Exception e) {
            log.error("Failed to process ApplicationCreated for application {}: {}", event.getApplicationId(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaTopics.OFFER_ACCEPTED, groupId = "notification-service-group")
    public void onOfferAccepted(OfferAcceptedEvent event) {
        try {
            orchestrationService.handleOfferAccepted(event);
        } catch (Exception e) {
            log.error("Failed to process OfferAccepted for application {}: {}", event.getApplicationId(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaTopics.OFFER_REJECTED, groupId = "notification-service-group")
    public void onOfferRejected(OfferRejectedEvent event) {
        try {
            orchestrationService.handleOfferRejected(event);
        } catch (Exception e) {
            log.error("Failed to process OfferRejected for application {}: {}", event.getApplicationId(), e.getMessage(), e);
        }
    }
}
