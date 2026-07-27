package com.cadence.aiinterviewservice.dto.response;

import com.cadence.aiinterviewservice.constants.InterviewSessionStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Backs the AI Interviews "Interview queue" table. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewQueueItemResponse {
    private UUID applicationId;
    private UUID candidateId;
    private String fullName;
    private String email;
    private UUID jobId;
    private String jobTitle;
    private InterviewSessionStatus status;
    private LocalDateTime invitedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime expiresAt;
}
