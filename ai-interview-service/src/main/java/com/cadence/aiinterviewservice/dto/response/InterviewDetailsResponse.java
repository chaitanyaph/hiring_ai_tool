package com.cadence.aiinterviewservice.dto.response;

import com.cadence.aiinterviewservice.constants.InterviewSessionStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Candidate-facing: backs the interview intro/setup screen. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewDetailsResponse {
    private UUID applicationId;
    private String jobTitle;
    private InterviewSessionStatus status;
    private int totalQuestions;
    private int estimatedDurationMinutes;
    private List<String> modeOptions;
    private LocalDateTime expiresAt;
}
