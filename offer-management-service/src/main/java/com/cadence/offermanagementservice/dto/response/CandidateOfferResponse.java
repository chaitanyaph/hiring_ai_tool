package com.cadence.offermanagementservice.dto.response;

import com.cadence.offermanagementservice.constants.OfferStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Backs #csec-offers ("My offer"). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateOfferResponse {
    private UUID id;
    private String jobTitle;
    private String companyName;
    private OfferStatus status;
    private BigDecimal baseSalary;
    private BigDecimal variableBonus;
    private BigDecimal esopEquity;
    private BigDecimal totalCtc;
    private LocalDate startDate;
    private LocalDate expiryDate;
    private Long daysUntilExpiry;
}
