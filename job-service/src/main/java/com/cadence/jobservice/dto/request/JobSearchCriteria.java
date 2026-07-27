package com.cadence.jobservice.dto.request;

import com.cadence.jobservice.constant.EmploymentType;
import com.cadence.jobservice.constant.JobStatus;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobSearchCriteria {
    private String title;
    private UUID departmentId;
    private String location;
    private JobStatus status;
    private EmploymentType employmentType;
    private UUID recruiterId;
    private UUID hiringManagerId;
    private LocalDate createdFrom;
    private LocalDate createdTo;
}
