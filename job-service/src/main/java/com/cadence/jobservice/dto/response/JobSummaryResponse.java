package com.cadence.jobservice.dto.response;

import com.cadence.jobservice.constant.EmploymentType;
import com.cadence.jobservice.constant.JobStatus;
import com.cadence.jobservice.constant.WorkType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Backs one row of the Jobs listing table. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobSummaryResponse {
    private UUID id;
    private String jobCode;
    private String title;
    private UUID departmentId;
    private String departmentName;
    private String location;
    private WorkType workType;
    private EmploymentType employmentType;
    private Integer numberOfOpenings;
    private long applicantsCount;
    private JobStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
}
