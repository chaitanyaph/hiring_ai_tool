package com.cadence.candidateservice.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobPreferencesResponse {
    private String preferredWorkType;
    private String preferredEmploymentType;
    private BigDecimal expectedSalary;
    private String salaryCurrency;
    private String noticePeriod;
    private String preferredLocations;
}
