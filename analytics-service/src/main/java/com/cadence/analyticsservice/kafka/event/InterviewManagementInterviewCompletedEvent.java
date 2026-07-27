package com.cadence.analyticsservice.kafka.event;

import lombok.*;

import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InterviewManagementInterviewCompletedEvent {
    private UUID interviewId;
    private UUID applicationId;
    private UUID candidateId;
    private String roundType;
    private Integer overallRating;
    private String recommendation;
}
