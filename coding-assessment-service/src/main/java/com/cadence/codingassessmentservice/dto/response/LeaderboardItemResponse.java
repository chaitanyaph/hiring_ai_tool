package com.cadence.codingassessmentservice.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Backs the "Candidate ranking" card on both Tab 3 and Tab 4, and doubles for Tab 3's "Completed assessments" list (completedAt populated, rank unused there). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaderboardItemResponse {
    private int rank;
    private UUID applicationId;
    private String candidateName;
    private String jobTitle;
    private Integer score;
    private Integer timeUsedSeconds;
    private LocalDateTime completedAt;
}
