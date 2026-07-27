package com.cadence.analyticsservice.kafka.consumer;

import com.cadence.analyticsservice.constants.KafkaTopics;
import com.cadence.analyticsservice.kafka.event.*;
import com.cadence.analyticsservice.service.MetricIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OfferManagementEventConsumer {

    private final MetricIngestionService metricIngestionService;

    @KafkaListener(topics = KafkaTopics.OFFER_GENERATED, groupId = "analytics-service-group")
    public void onOfferGenerated(OfferGeneratedEvent event) {
        try {
            metricIngestionService.onOfferGenerated(event);
        } catch (Exception e) {
            log.error("Failed to process OfferGenerated for offer {}: {}", event.getOfferId(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaTopics.OFFER_SENT, groupId = "analytics-service-group")
    public void onOfferSent(OfferSentEvent event) {
        try {
            metricIngestionService.onOfferSent(event);
        } catch (Exception e) {
            log.error("Failed to process OfferSent for offer {}: {}", event.getOfferId(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaTopics.OFFER_ACCEPTED, groupId = "analytics-service-group")
    public void onOfferAccepted(OfferAcceptedEvent event) {
        try {
            metricIngestionService.onOfferAccepted(event);
        } catch (Exception e) {
            log.error("Failed to process OfferAccepted for offer {}: {}", event.getOfferId(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaTopics.OFFER_REJECTED, groupId = "analytics-service-group")
    public void onOfferRejected(OfferRejectedEvent event) {
        try {
            metricIngestionService.onOfferRejected(event);
        } catch (Exception e) {
            log.error("Failed to process OfferRejected for offer {}: {}", event.getOfferId(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaTopics.OFFER_NEGOTIATION_REQUESTED, groupId = "analytics-service-group")
    public void onOfferNegotiationRequested(OfferNegotiationRequestedEvent event) {
        try {
            metricIngestionService.onOfferNegotiationRequested(event);
        } catch (Exception e) {
            log.error("Failed to process OfferNegotiationRequested for offer {}: {}", event.getOfferId(), e.getMessage(), e);
        }
    }
}
