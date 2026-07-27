package com.cadence.companyservice.kafka.producer;

import com.cadence.companyservice.constant.KafkaTopics;
import com.cadence.companyservice.kafka.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Thin wrapper around KafkaTemplate. Every publish method is @Async:
 * KafkaTemplate.send() blocks the calling thread while resolving broker
 * metadata *before* it returns a future, so wrapping the future in
 * .whenComplete() alone does not make this non-blocking -- the method
 * itself must run off the request thread. Failures are logged, never
 * thrown, so a Kafka outage never breaks a company/department/office/
 * invitation write.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompanyEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Async
    public void publishCompanyCreated(CompanyCreatedEvent event) {
        publish(KafkaTopics.COMPANY_CREATED, event.getCompanyId().toString(), event);
    }

    @Async
    public void publishCompanyUpdated(CompanyUpdatedEvent event) {
        publish(KafkaTopics.COMPANY_UPDATED, event.getCompanyId().toString(), event);
    }

    @Async
    public void publishDepartmentCreated(DepartmentCreatedEvent event) {
        publish(KafkaTopics.DEPARTMENT_CREATED, event.getDepartmentId().toString(), event);
    }

    @Async
    public void publishDepartmentUpdated(DepartmentUpdatedEvent event) {
        publish(KafkaTopics.DEPARTMENT_UPDATED, event.getDepartmentId().toString(), event);
    }

    @Async
    public void publishDepartmentDeleted(DepartmentDeletedEvent event) {
        publish(KafkaTopics.DEPARTMENT_DELETED, event.getDepartmentId().toString(), event);
    }

    @Async
    public void publishOfficeCreated(OfficeCreatedEvent event) {
        publish(KafkaTopics.OFFICE_CREATED, event.getOfficeId().toString(), event);
    }

    @Async
    public void publishOfficeUpdated(OfficeUpdatedEvent event) {
        publish(KafkaTopics.OFFICE_UPDATED, event.getOfficeId().toString(), event);
    }

    @Async
    public void publishOfficeDeleted(OfficeDeletedEvent event) {
        publish(KafkaTopics.OFFICE_DELETED, event.getOfficeId().toString(), event);
    }

    @Async
    public void publishTeamInvitationCreated(TeamInvitationCreatedEvent event) {
        publish(KafkaTopics.TEAM_INVITATION_CREATED, event.getInvitationId().toString(), event);
    }

    @Async
    public void publishTeamInvitationCancelled(TeamInvitationCancelledEvent event) {
        publish(KafkaTopics.TEAM_INVITATION_CANCELLED, event.getInvitationId().toString(), event);
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
