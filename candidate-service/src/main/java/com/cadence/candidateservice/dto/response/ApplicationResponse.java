package com.cadence.candidateservice.dto.response;

import com.cadence.candidateservice.constant.ApplicationStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationResponse {
    private UUID id;
    private UUID jobId;
    private UUID companyId;
    private String jobTitleSnapshot;
    private String companyNameSnapshot;
    private String locationSnapshot;
    private String employmentTypeSnapshot;
    private ApplicationStatus status;
    private Integer matchScore;
    private LocalDateTime appliedAt;
    private LocalDateTime withdrawnAt;

    /** Only populated on the single-application detail endpoint, not on list views. */
    private List<ApplicationStatusHistoryResponse> history;
}
