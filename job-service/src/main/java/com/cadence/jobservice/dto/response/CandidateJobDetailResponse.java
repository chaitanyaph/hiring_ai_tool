package com.cadence.jobservice.dto.response;

import com.cadence.jobservice.constant.EmploymentType;
import com.cadence.jobservice.constant.JobStatus;
import com.cadence.jobservice.constant.WorkType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** Backs the candidate-facing Job Details page -- a published (or since-closed/expired) job, viewed by a candidate. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateJobDetailResponse {
    private UUID id;
    private String title;
    private UUID companyId;
    private String companyName;
    private String departmentName;
    private String location;
    private WorkType workType;
    private EmploymentType employmentType;
    private Integer numberOfOpenings;
    private LocalDate applicationDeadline;
    private JobStatus status;
    private String descriptionHtml;
    private JobRequirementsResponse requirements;
    private LocalDateTime publishedAt;
    private LocalDateTime closedAt;
    private LocalDateTime archivedAt;
}
