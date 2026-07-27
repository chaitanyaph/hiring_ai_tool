package com.cadence.applicationservice.dto.request;

import com.cadence.applicationservice.constant.ApplicationStage;
import com.cadence.applicationservice.constant.ApplicationStatus;
import com.cadence.applicationservice.constant.Priority;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationSearchCriteria {
    private UUID companyId;
    private UUID jobId;
    private UUID candidateId;
    private UUID recruiterId;
    private UUID hiringManagerId;
    private ApplicationStatus status;
    private ApplicationStage stage;
    private Priority priority;
    private String candidateName;
    private String candidateEmail;
    private String jobTitle;
    private Integer minOverallScore;
    private LocalDateTime appliedFrom;
    private LocalDateTime appliedTo;
}
