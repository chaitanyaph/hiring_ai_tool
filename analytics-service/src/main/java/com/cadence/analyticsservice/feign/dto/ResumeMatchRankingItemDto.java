package com.cadence.analyticsservice.feign.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResumeMatchRankingItemDto {
    private UUID applicationId;
    private UUID candidateId;
    private String fullName;
    private String status;
    private Integer overallMatchScore;
}
