package com.cadence.offermanagementservice.dto.response;

import lombok.*;

/** Backs the 4 recruiter dashboard KPI cards -- added beyond the literal API list since the Figma clearly requires it. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfferDashboardStatsResponse {
    private long offersSent;
    private double acceptanceRatePercent;
    private double avgTimeToAcceptDays;
    private long pendingApprovalCount;
}
