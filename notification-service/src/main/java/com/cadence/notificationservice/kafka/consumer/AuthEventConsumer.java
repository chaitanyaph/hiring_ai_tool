package com.cadence.notificationservice.kafka.consumer;

import com.cadence.notificationservice.constants.KafkaTopics;
import com.cadence.notificationservice.kafka.event.PasswordResetRequestedEvent;
import com.cadence.notificationservice.kafka.event.UserRegisteredEvent;
import com.cadence.notificationservice.service.NotificationOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** A malformed event never crashes the consumer thread or blocks the partition -- same defensive posture as every sibling service. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthEventConsumer {

    private final NotificationOrchestrationService orchestrationService;

    @KafkaListener(topics = KafkaTopics.USER_REGISTERED, groupId = "notification-service-group")
    public void onUserRegistered(UserRegisteredEvent event) {
        try {
            orchestrationService.handleUserRegistered(event);
        } catch (Exception e) {
            log.error("Failed to process UserRegistered for user {}: {}", event.getUserId(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaTopics.PASSWORD_RESET_REQUESTED, groupId = "notification-service-group")
    public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
        try {
            orchestrationService.handlePasswordResetRequested(event);
        } catch (Exception e) {
            log.error("Failed to process PasswordResetRequested for user {}: {}", event.getUserId(), e.getMessage(), e);
        }
    }
}
