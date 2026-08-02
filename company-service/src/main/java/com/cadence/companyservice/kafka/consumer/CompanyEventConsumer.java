package com.cadence.companyservice.kafka.consumer;

import com.cadence.companyservice.constant.KafkaTopics;
import com.cadence.companyservice.entity.Company;
import com.cadence.companyservice.kafka.event.InvitationAcceptedEvent;
import com.cadence.companyservice.kafka.event.UserCreatedEvent;
import com.cadence.companyservice.kafka.event.UserRegisteredEvent;
import com.cadence.companyservice.repository.CompanyRepository;
import com.cadence.companyservice.service.TeamInvitationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    private final CompanyRepository companyRepository;

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

    /**
     * Pre-seeds this service's own Company row when a COMPANY_ADMIN registers.
     * Auth Service creates its own company record (the source of the companyId
     * carried in every JWT) in its own database -- without this, this service's
     * GET /companies/{id} 404s for every newly registered workspace since it never
     * otherwise learns the company exists.
     */
    @KafkaListener(topics = KafkaTopics.USER_REGISTERED, groupId = "company-service-group")
    @Transactional
    public void onUserRegistered(UserRegisteredEvent event) {
        try {
            if (!"COMPANY_ADMIN".equals(event.getUserType()) || event.getCompanyId() == null) {
                return;
            }
            if (companyRepository.existsById(event.getCompanyId())) {
                return;
            }
            Company company = Company.builder()
                    .id(event.getCompanyId())
                    .companyName(event.getCompanyName())
                    .companySlug(event.getCompanySlug())
                    .build();
            companyRepository.save(company);
        } catch (Exception e) {
            log.error("Failed to pre-seed company {} from registration: {}", event.getCompanyId(), e.getMessage(), e);
        }
    }
}
