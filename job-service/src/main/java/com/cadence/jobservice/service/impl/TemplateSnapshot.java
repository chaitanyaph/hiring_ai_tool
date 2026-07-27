package com.cadence.jobservice.service.impl;

import com.cadence.jobservice.constant.EmploymentType;
import com.cadence.jobservice.constant.SkillType;
import com.cadence.jobservice.constant.WorkType;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Internal JSON shape stored in job_template.template_data_json -- a
 * template is just data to pre-fill a new draft with, never queried on
 * its own fields, so this doesn't need to be a normalized schema.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class TemplateSnapshot {
    private String location;
    private WorkType workType;
    private EmploymentType employmentType;
    private Integer numberOfOpenings;
    private String descriptionHtml;

    private Integer minExperienceYears;
    private Integer maxExperienceYears;
    private String education;
    private String certifications;
    private String languages;
    private BigDecimal minSalary;
    private BigDecimal maxSalary;
    private String salaryCurrency;
    private Integer noticePeriodDays;
    private String responsibilities;
    private List<String> benefits;
    private List<SkillSnapshot> skills;
    private List<StageSnapshot> stages;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    static class SkillSnapshot {
        private String skillName;
        private SkillType skillType;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    static class StageSnapshot {
        private String stageName;
        private Integer stageOrder;
        private boolean enabled;
    }
}
