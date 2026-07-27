package com.cadence.companyservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * Interface only, per architecture: Company Service does not call
 * Notification Service synchronously today -- all notification triggers
 * go through Kafka events. This is scaffolding for a future manual
 * "resend now" admin action that bypasses the event bus.
 */
@FeignClient(name = "notification-service")
public interface NotificationServiceClient {

    @PostMapping("/api/v1/notifications/team-invitation")
    void sendTeamInvitationEmail(@RequestBody Map<String, Object> payload);
}
