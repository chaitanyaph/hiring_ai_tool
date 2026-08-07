package com.cadence.notificationservice.feign.dto;

import lombok.*;

import java.util.UUID;

/** Mirrors Application Service's ApplicationResponse -- only the fields this service needs. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationSummaryDto {
    private UUID id;
    private UUID jobId;
    private UUID candidateId;
    private UUID companyId;
    private String candidateNameSnapshot;
    private String candidateEmailSnapshot;
    private String jobTitleSnapshot;
    private String currentStatus;
    private UUID assignedRecruiterId;
}
