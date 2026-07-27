package com.cadence.interviewmanagementservice.dto.response;

import com.cadence.interviewmanagementservice.constants.InterviewMode;
import com.cadence.interviewmanagementservice.constants.InterviewStatus;
import com.cadence.interviewmanagementservice.constants.RoundType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/** Backs #csec-interviews / #drawer-cand-interview-detail (§A8, §A9). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateInterviewResponse {
    private UUID id;
    private String jobTitle;
    private String companyName;
    private RoundType roundType;
    private InterviewStatus status;
    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    private InterviewMode mode;
    private String meetingLink;
    private List<String> interviewerNames;
    private boolean upcoming;
}
