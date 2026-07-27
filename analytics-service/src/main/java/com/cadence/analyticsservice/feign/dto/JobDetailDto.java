package com.cadence.analyticsservice.feign.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JobDetailDto {
    private UUID id;
    private String title;
    private UUID companyId;
    private UUID recruiterId;
}
