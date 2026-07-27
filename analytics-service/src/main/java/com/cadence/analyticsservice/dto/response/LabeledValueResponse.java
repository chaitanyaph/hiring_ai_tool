package com.cadence.analyticsservice.dto.response;

import lombok.*;

import java.math.BigDecimal;

/** Reusable for any label->value breakdown: source channel, skill demand, language usage, shortlist decision, etc. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabeledValueResponse {
    private String label;
    private BigDecimal value;
}
