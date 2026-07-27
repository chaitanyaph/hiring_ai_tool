package com.cadence.interviewmanagementservice.kafka.event;

import lombok.*;

import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InterviewCancelledEvent {
    private UUID interviewId;
    private UUID applicationId;
    private String reason;
}
