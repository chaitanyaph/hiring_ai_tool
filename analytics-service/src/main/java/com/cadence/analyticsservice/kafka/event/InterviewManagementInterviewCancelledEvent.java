package com.cadence.analyticsservice.kafka.event;

import lombok.*;

import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InterviewManagementInterviewCancelledEvent {
    private UUID interviewId;
    private UUID applicationId;
    private String reason;
}
