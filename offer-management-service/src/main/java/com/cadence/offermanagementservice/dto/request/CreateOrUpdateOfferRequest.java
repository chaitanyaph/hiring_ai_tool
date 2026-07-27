package com.cadence.offermanagementservice.dto.request;

import com.cadence.offermanagementservice.constants.EmploymentType;
import com.cadence.offermanagementservice.constants.SendMode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Matches the Figma's 4-step wizard fields exactly (steps 1-3 combined into one submission; step 4 is the sendMode). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrUpdateOfferRequest {

    @NotNull
    private UUID applicationId;

    @NotNull
    private UUID jobId;

    @NotNull
    private UUID candidateId;

    private String department;

    @NotNull
    private EmploymentType employmentType;

    @NotNull
    private LocalDate startDate;

    @NotNull
    @PositiveOrZero
    private BigDecimal baseSalary;

    @PositiveOrZero
    private BigDecimal variableBonus;

    @PositiveOrZero
    private BigDecimal esopEquity;

    private List<String> benefits;

    private UUID approverId;

    private LocalDate expiryDate;

    @Builder.Default
    private SendMode sendMode = SendMode.DRAFT;
}
