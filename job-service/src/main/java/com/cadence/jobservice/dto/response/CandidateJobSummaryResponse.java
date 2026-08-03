package com.cadence.jobservice.dto.response;

import com.cadence.jobservice.constant.EmploymentType;
import com.cadence.jobservice.constant.WorkType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Backs one card of the candidate-facing "Browse jobs" screen -- published jobs across every company. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateJobSummaryResponse {
    private UUID id;
    private String title;
    private UUID companyId;
    private String companyName;
    private String departmentName;
    private String location;
    private WorkType workType;
    private EmploymentType employmentType;
    private List<String> skills;
    private BigDecimal minSalary;
    private BigDecimal maxSalary;
    private String salaryCurrency;
    private LocalDateTime publishedAt;
}
