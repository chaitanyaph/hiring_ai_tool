package com.cadence.notificationservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Mirrors auth-service's exact shape -- verificationLink resolves the "EmailVerificationRequested doesn't exist" gap without inventing a new topic. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserRegisteredEvent {
    private UUID userId;
    private String fullName;
    private String email;
    private String userType;
    private UUID companyId;
    private String verificationLink;
    private LocalDateTime occurredAt;
}
