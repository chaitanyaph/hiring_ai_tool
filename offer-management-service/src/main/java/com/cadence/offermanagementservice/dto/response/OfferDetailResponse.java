package com.cadence.offermanagementservice.dto.response;

import com.cadence.offermanagementservice.constants.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Backs #drawer-offer-detail (candidate/role/compensation/approval-timeline). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfferDetailResponse {
    private UUID id;
    private UUID applicationId;
    private UUID candidateId;
    private String candidateName;
    private String candidateEmail;
    private UUID jobId;
    private String jobTitle;
    private String companyName;
    private String department;
    private EmploymentType employmentType;
    private LocalDate startDate;
    private BigDecimal baseSalary;
    private BigDecimal variableBonus;
    private BigDecimal esopEquity;
    private BigDecimal totalCtc;
    private List<String> benefits;
    private UUID approverId;
    private ApprovalStatus approvalStatus;
    private String approvalNotes;
    private LocalDate expiryDate;
    private OfferStatus status;
    private boolean documentGenerated;
    private List<ActivityLogResponse> timeline;
    private List<NegotiationResponse> negotiations;
}
