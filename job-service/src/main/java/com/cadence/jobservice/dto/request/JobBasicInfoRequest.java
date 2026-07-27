package com.cadence.jobservice.dto.request;

import com.cadence.jobservice.constant.EmploymentType;
import com.cadence.jobservice.constant.WorkType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Wizard Step 1 -- Basic info. Deliberately lenient: only title is
 * required so "Save Draft"/autosave can persist a half-filled step; the
 * stricter cross-field checks (openings > 0, deadline not in the past)
 * only block Publish, not every intermediate save.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobBasicInfoRequest {

    @NotBlank(message = "Job title is required")
    @Size(max = 150)
    private String title;

    private UUID departmentId;

    @Min(value = 0, message = "Number of openings cannot be negative")
    private Integer numberOfOpenings;

    @Size(max = 200)
    private String location;

    private WorkType workType;

    private EmploymentType employmentType;

    private LocalDate applicationDeadline;

    private String descriptionHtml;
}
