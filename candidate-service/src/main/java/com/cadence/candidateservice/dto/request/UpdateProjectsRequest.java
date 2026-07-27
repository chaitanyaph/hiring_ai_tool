package com.cadence.candidateservice.dto.request;

import jakarta.validation.Valid;
import lombok.*;

import java.util.List;

/** Wizard Step 6 -- full replace. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProjectsRequest {
    @Valid
    private List<ProjectItemRequest> items;
}
