package com.cadence.jobservice.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobRequirementsResponse {
    private Integer minExperienceYears;
    private Integer maxExperienceYears;
    private List<SkillResponse> skills;
    private String education;
    private String certifications;
    private String languages;
    private BigDecimal minSalary;
    private BigDecimal maxSalary;
    private String salaryCurrency;
    private Integer noticePeriodDays;
    private String responsibilities;
    private List<String> benefits;
}
