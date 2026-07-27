package com.cadence.analyticsservice.dto.response;

import lombok.*;

import java.util.List;

/** Backs GET /api/v1/funnel -- matches sec-analytics's job/range-filtered funnel exactly. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FunnelResponse {
    private String scope;
    private List<FunnelStageResponse> stages;
}
