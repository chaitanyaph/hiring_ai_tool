package com.cadence.aiinterviewservice.kafka.event;

import com.cadence.aiinterviewservice.constants.InterviewMode;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InterviewStartedEvent {
    private UUID applicationId;
    private UUID jobId;
    private UUID candidateId;
    private UUID sessionId;
    private InterviewMode mode;
    private LocalDateTime occurredAt;
}
