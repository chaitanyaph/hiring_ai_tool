package com.cadence.jobservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/** Wizard Step 2 -- Requirements. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobRequirementsRequest {

    @Min(value = 0, message = "Minimum experience cannot be negative")
    private Integer minExperienceYears;

    @Min(value = 0, message = "Maximum experience cannot be negative")
    private Integer maxExperienceYears;

    @Valid
    private List<SkillRequest> skills;

    private String education;

    private String certifications;

    private String languages;

    @DecimalMin(value = "0", message = "Minimum salary cannot be negative")
    private BigDecimal minSalary;

    @DecimalMin(value = "0", message = "Maximum salary cannot be negative")
    private BigDecimal maxSalary;

    private String salaryCurrency;

    @Min(value = 0, message = "Notice period cannot be negative")
    private Integer noticePeriodDays;

    private String responsibilities;

    private List<@NotBlank String> benefits;
}
