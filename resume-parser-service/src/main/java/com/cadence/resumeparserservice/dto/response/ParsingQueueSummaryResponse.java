package com.cadence.resumeparserservice.dto.response;

import lombok.*;

/** Backs the #sec-parsing KPI row (Queued / Processing / Parsed today / Failed). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParsingQueueSummaryResponse {
    private long queuedCount;
    private long processingCount;
    private long parsedTodayCount;
    private long failedCount;
}
