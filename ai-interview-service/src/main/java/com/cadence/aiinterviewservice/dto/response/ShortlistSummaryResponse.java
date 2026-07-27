package com.cadence.aiinterviewservice.dto.response;

import lombok.*;

/** Backs the AI Shortlisting KPI row (Shortlisted / Rejected / Manual review / Auto-shortlist rate). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShortlistSummaryResponse {
    private long shortlistedCount;
    private long rejectedCount;
    private long manualReviewCount;
    private double autoShortlistRatePercent;
}
