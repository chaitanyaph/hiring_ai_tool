package com.cadence.codingassessmentservice.feign.dto;

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
    private UUID companyId;
    private UUID departmentId;
}
