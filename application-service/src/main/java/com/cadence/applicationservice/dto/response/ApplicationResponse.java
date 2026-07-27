package com.cadence.applicationservice.dto.response;

import com.cadence.applicationservice.constant.ApplicationStage;
import com.cadence.applicationservice.constant.ApplicationStatus;
import com.cadence.applicationservice.constant.Priority;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationResponse {
    private UUID id;
    private UUID companyId;
    private UUID jobId;
    private UUID candidateId;
    private UUID resumeId;
    private UUID assignedRecruiterId;
    private UUID assignedHiringManagerId;
    private String candidateNameSnapshot;
    private String candidateEmailSnapshot;
    private String jobTitleSnapshot;
    private ApplicationStatus currentStatus;
    private ApplicationStage currentStage;
    private Integer resumeMatchScore;
    private Integer aiInterviewScore;
    private Integer codingScore;
    private Integer overallScore;
    private Priority priority;
    private String remarks;
    private LocalDateTime appliedAt;
    private LocalDateTime lastStatusChangedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;

    /** Only populated on the single-application detail endpoint, not on list/search results. */
    private List<NoteResponse> notes;
}
