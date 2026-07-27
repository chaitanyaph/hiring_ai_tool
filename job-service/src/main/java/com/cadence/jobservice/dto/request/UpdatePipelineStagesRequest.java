package com.cadence.jobservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

/** Wizard Step 3 -- Hiring stages. Full replace semantics: the client sends the complete, reordered list. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePipelineStagesRequest {

    @NotEmpty(message = "At least one pipeline stage is required")
    @Valid
    private List<PipelineStageRequest> stages;
}
