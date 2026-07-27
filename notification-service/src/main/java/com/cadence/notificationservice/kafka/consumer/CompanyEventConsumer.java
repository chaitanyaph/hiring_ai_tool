package com.cadence.notificationservice.kafka.consumer;

import com.cadence.notificationservice.constants.KafkaTopics;
import com.cadence.notificationservice.kafka.event.CompanyCreatedEvent;
import com.cadence.notificationservice.kafka.event.TeamInvitationCreatedEvent;
import com.cadence.notificationservice.service.NotificationOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompanyEventConsumer {

    private final NotificationOrchestrationService orchestrationService;

    @KafkaListener(topics = KafkaTopics.COMPANY_CREATED, groupId = "notification-service-group")
    public void onCompanyCreated(CompanyCreatedEvent event) {
        try {
            orchestrationService.handleCompanyCreated(event);
        } catch (Exception e) {
            log.error("Failed to process CompanyCreated for company {}: {}", event.getCompanyId(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaTopics.TEAM_INVITATION_CREATED, groupId = "notification-service-group")
    public void onTeamInvitationCreated(TeamInvitationCreatedEvent event) {
        try {
            orchestrationService.handleTeamInvitationCreated(event);
        } catch (Exception e) {
            log.error("Failed to process TeamInvitationCreated for invitation {}: {}", event.getInvitationId(), e.getMessage(), e);
        }
    }
}
