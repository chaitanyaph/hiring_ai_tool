package com.cadence.notificationservice.kafka.consumer;

import com.cadence.notificationservice.constants.KafkaTopics;
import com.cadence.notificationservice.kafka.event.OfferSentEvent;
import com.cadence.notificationservice.service.NotificationOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OfferManagementEventConsumer {

    private final NotificationOrchestrationService orchestrationService;

    @KafkaListener(topics = KafkaTopics.OFFER_SENT, groupId = "notification-service-group")
    public void onOfferSent(OfferSentEvent event) {
        try {
            orchestrationService.handleOfferSent(event);
        } catch (Exception e) {
            log.error("Failed to process OfferSent for application {}: {}", event.getApplicationId(), e.getMessage(), e);
        }
    }
}
