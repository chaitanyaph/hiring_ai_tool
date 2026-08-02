package com.cadence.candidateservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Mirrors auth-service's own UserRegisteredEvent shape -- services don't share a Java type, only the JSON contract on the topic. */
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
