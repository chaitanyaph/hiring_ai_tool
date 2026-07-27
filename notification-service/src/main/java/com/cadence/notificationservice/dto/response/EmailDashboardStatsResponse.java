package com.cadence.notificationservice.dto.response;

import lombok.*;

import java.util.Map;

/** Backs notif-dashboard KPI cards (§A3). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailDashboardStatsResponse {
    private long sentToday;
    private double deliveryRatePercent;
    private long failedCount;
    private long scheduledUpcomingCount;
    private Map<String, Long> sentByTemplateCategory;
}
