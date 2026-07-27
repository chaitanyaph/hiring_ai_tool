package com.cadence.companyservice.kafka.consumer;

import com.cadence.companyservice.constant.KafkaTopics;
import com.cadence.companyservice.kafka.event.InvitationAcceptedEvent;
import com.cadence.companyservice.kafka.event.UserCreatedEvent;
import com.cadence.companyservice.service.TeamInvitationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Closes the invitation loop from the other side: Auth Service owns
 * account creation, this service only reacts to confirm the invitation
 * that led to it is done. Listener failures are logged, not rethrown --
 * an already-accepted or already-expired invitation is not a reason to
 * block the consumer or trigger a retry storm.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompanyEventConsumer {

    private final TeamInvitationService teamInvitationService;

    @KafkaListener(topics = KafkaTopics.USER_CREATED, groupId = "company-service-group")
    public void onUserCreated(UserCreatedEvent event) {
        try {
            if (event.getCompanyId() != null) {
                teamInvitationService.markAcceptedByEmail(event.getCompanyId(), event.getEmail());
            }
        } catch (Exception e) {
            log.warn("Could not reconcile UserCreated event for {}: {}", event.getEmail(), e.getMessage());
        }
    }

    @KafkaListener(topics = KafkaTopics.INVITATION_ACCEPTED, groupId = "company-service-group")
    public void onInvitationAccepted(InvitationAcceptedEvent event) {
        try {
            teamInvitationService.markAcceptedByToken(event.getInviteToken());
        } catch (Exception e) {
            log.warn("Could not reconcile InvitationAccepted event for token {}: {}", event.getInviteToken(), e.getMessage());
        }
    }
}
