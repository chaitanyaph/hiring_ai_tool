package com.cadence.analyticsservice.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/** Backs sec-analytics's "Recruiter performance" table: Recruiter | Open reqs | Hires this quarter | Avg. time to hire. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecruiterPerformanceResponse {
    private UUID recruiterId;
    private String recruiterName;
    private int openReqs;
    private int applicationsReviewed;
    private int hiresCount;
    private BigDecimal avgTimeToHireDays;
    private BigDecimal avgInterviewRating;
    private BigDecimal avgOfferAcceptancePct;
}
