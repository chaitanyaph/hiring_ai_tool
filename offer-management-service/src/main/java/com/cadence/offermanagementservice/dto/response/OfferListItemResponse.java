package com.cadence.offermanagementservice.dto.response;

import com.cadence.offermanagementservice.constants.OfferStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Backs #offer-tbody rows: Candidate / Job / CTC / Status / Updated. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfferListItemResponse {
    private UUID id;
    private String candidateName;
    private String candidateEmail;
    private String jobTitle;
    private BigDecimal totalCtc;
    private OfferStatus status;
    private LocalDateTime updatedAt;
}
