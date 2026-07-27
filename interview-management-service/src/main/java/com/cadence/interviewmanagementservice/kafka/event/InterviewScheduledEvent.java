package com.cadence.interviewmanagementservice.kafka.event;

import com.cadence.interviewmanagementservice.constants.RoundType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InterviewScheduledEvent {
    private UUID interviewId;
    private UUID applicationId;
    private UUID candidateId;
    private UUID jobId;
    private RoundType roundType;
    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
}
