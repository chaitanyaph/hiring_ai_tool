package com.cadence.aiinterviewservice.feign.dto;

import lombok.*;

import java.util.UUID;

/** Mirrors Job Service's JobDetailResponse -- only the fields this service needs. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JobDetailDto {
    private UUID id;
    private String title;
    private UUID departmentId;
    private UUID companyId;
    private JobRequirementsDto requirements;
}
