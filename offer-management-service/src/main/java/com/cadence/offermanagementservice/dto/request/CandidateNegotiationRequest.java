package com.cadence.offermanagementservice.dto.request;

import lombok.*;

import java.math.BigDecimal;

/** Zero Figma UI coverage -- built because request-negotiation is explicitly listed in the required Candidate API set. See README. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateNegotiationRequest {
    private BigDecimal proposedCtc;
    private String message;
}
