package com.cadence.applicationservice.dto.response;

import lombok.*;

import java.util.List;

/** Backs GET /applications/{id}/history -- the raw structured audit trail. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationHistoryResponse {
    private List<StatusHistoryResponse> statusHistory;
    private List<StageHistoryResponse> stageHistory;
}
