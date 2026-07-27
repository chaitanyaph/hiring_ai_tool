package com.cadence.notificationservice.kafka.event;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InterviewRescheduledEvent {
    private UUID interviewId;
    private UUID applicationId;
    private LocalDate newScheduledDate;
    private LocalTime newScheduledTime;
    private String reason;
}
