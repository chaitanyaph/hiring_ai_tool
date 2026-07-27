package com.cadence.interviewmanagementservice.dto.response;

import com.cadence.interviewmanagementservice.constants.InterviewStatus;
import com.cadence.interviewmanagementservice.constants.RoundType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/** Backs #interview-upcoming / #interview-completed rows (§A1). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewListItemResponse {
    private UUID id;
    private UUID applicationId;
    private UUID candidateId;
    private String candidateName;
    private UUID jobId;
    private String jobTitle;
    private RoundType roundType;
    private InterviewStatus status;
    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    private int durationMinutes;
    private List<PanelistResponse> panelists;
    private boolean feedbackSubmitted;
}
