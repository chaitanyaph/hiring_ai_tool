package com.cadence.jobservice.dto.response;

import com.cadence.jobservice.constant.EmploymentType;
import com.cadence.jobservice.constant.JobStatus;
import com.cadence.jobservice.constant.WorkType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Backs the Review & Publish step (Step 4) and the single-job GET. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobDetailResponse {
    private UUID id;
    private String jobCode;
    private String title;
    private UUID companyId;
    private UUID departmentId;
    private String location;
    private WorkType workType;
    private EmploymentType employmentType;
    private Integer numberOfOpenings;
    private LocalDate applicationDeadline;
    private JobStatus status;
    private String descriptionHtml;
    private JobRequirementsResponse requirements;
    private List<PipelineStageResponse> pipelineStages;
    private UUID recruiterId;
    private UUID hiringManagerId;
    private LocalDateTime publishedAt;
    private LocalDateTime closedAt;
    private LocalDateTime archivedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;
}
