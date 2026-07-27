package com.cadence.candidateservice.dto.request;

import lombok.*;

import java.math.BigDecimal;

/** Wizard Step 9. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobPreferencesRequest {
    private String preferredWorkType;
    private String preferredEmploymentType;
    private BigDecimal expectedSalary;
    private String salaryCurrency;
    private String noticePeriod;
    private String preferredLocations;
}
