package com.cadence.authservice.kafka.producer;

import com.cadence.authservice.constant.KafkaTopics;
import com.cadence.authservice.kafka.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Thin wrapper around KafkaTemplate. All publishes are fire-and-forget
 * from the caller's perspective (auth flows must not block on Kafka),
 * but failures are logged loudly since a lost UserRegisteredEvent means
 * Notification Service never sends the verification email.
 *
 * The public publishXxx methods are @Async: KafkaTemplate.send() itself
 * blocks the calling thread for up to max.block.ms while it resolves
 * broker/topic metadata, which happens *before* it returns the future
 * that .whenComplete() below operates on -- so without @Async here, an
 * unreachable broker would still stall the caller (e.g. register()) for
 * up to a minute despite the future being handled asynchronously.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Async
    public void publishUserRegistered(UserRegisteredEvent event) {
        publish(KafkaTopics.USER_REGISTERED, event.getUserId().toString(), event);
    }

    @Async
    public void publishUserLoggedIn(UserLoggedInEvent event) {
        publish(KafkaTopics.USER_LOGGED_IN, event.getUserId().toString(), event);
    }

    @Async
    public void publishPasswordResetRequested(PasswordResetRequestedEvent event) {
        publish(KafkaTopics.PASSWORD_RESET_REQUESTED, event.getUserId().toString(), event);
    }

    @Async
    public void publishPasswordChanged(PasswordChangedEvent event) {
        publish(KafkaTopics.PASSWORD_CHANGED, event.getUserId().toString(), event);
    }

    @Async
    public void publishAccountLocked(AccountLockedEvent event) {
        publish(KafkaTopics.ACCOUNT_LOCKED, event.getUserId().toString(), event);
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
