package com.cadence.offermanagementservice.dto.response;

import com.cadence.offermanagementservice.constants.NegotiationStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NegotiationResponse {
    private UUID id;
    private BigDecimal proposedCtc;
    private String message;
    private NegotiationStatus status;
    private String recruiterNotes;
    private LocalDateTime requestedAt;
    private LocalDateTime respondedAt;
}
