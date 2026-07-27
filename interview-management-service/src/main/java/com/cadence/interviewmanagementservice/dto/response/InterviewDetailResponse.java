package com.cadence.interviewmanagementservice.dto.response;

import com.cadence.interviewmanagementservice.constants.InterviewMode;
import com.cadence.interviewmanagementservice.constants.InterviewStatus;
import com.cadence.interviewmanagementservice.constants.RoundType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/** Backs #drawer-interview-detail (§A3). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewDetailResponse {
    private UUID id;
    private UUID applicationId;
    private UUID candidateId;
    private String candidateName;
    private UUID jobId;
    private String jobTitle;
    private String companyName;
    private RoundType roundType;
    private InterviewStatus status;
    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    private int durationMinutes;
    private InterviewMode mode;
    private String meetingLink;
    private List<PanelistResponse> panelists;
    private String notesForPanel;
    private String cancelReason;
    private String rescheduleReason;
    private boolean feedbackSubmittable;
}
