package com.cadence.analyticsservice.dto.response;

import lombok.*;

/** avgSalary/departmentWise/locationWise from the text spec are omitted -- no offer-management-service event carries CTC, department, or location, and no Feign client exists to that service (no internal endpoint there either). Flagged in README. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfferAnalyticsResponse {
    private long offersGenerated;
    private long offersSent;
    private long offersAccepted;
    private long offersRejected;
    private long negotiationRequestedCount;
    private Double acceptanceRatePercent;
    private Double negotiationRatePercent;
}
