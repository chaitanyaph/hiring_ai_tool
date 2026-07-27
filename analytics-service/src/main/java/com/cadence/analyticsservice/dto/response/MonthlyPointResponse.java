package com.cadence.analyticsservice.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyPointResponse {
    private String monthLabel;
    private long value;
}
