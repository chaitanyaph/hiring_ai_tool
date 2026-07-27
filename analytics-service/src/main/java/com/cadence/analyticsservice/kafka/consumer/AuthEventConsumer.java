package com.cadence.analyticsservice.kafka.consumer;

import com.cadence.analyticsservice.constants.KafkaTopics;
import com.cadence.analyticsservice.kafka.event.UserRegisteredEvent;
import com.cadence.analyticsservice.service.MetricIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthEventConsumer {

    private final MetricIngestionService metricIngestionService;

    @KafkaListener(topics = KafkaTopics.USER_REGISTERED, groupId = "analytics-service-group")
    public void onUserRegistered(UserRegisteredEvent event) {
        try {
            metricIngestionService.onUserRegistered(event);
        } catch (Exception e) {
            log.error("Failed to process UserRegistered for user {}: {}", event.getUserId(), e.getMessage(), e);
        }
    }
}
