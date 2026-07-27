package com.cadence.candidateservice.dto.request;

import com.cadence.candidateservice.constant.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/** Used by recruiting-side roles to advance/reject an application. Candidates never call this -- they only withdraw. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangeApplicationStageRequest {
    @NotNull(message = "toStatus is required")
    private ApplicationStatus toStatus;

    private Integer matchScore;
    private String note;
}
