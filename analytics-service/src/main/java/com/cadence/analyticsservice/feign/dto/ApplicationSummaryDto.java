package com.cadence.analyticsservice.feign.dto;

import lombok.*;

import java.util.UUID;

/** Mirrors Application Service's ApplicationResponse -- only the fields this service needs. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationSummaryDto {
    private UUID id;
    private UUID companyId;
    private UUID jobId;
    private UUID candidateId;
    private String currentStatus;
}
