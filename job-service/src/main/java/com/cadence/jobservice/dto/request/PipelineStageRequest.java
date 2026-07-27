package com.cadence.jobservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PipelineStageRequest {

    /** Null for a newly added custom stage; set when reordering/renaming/toggling an existing one. */
    private UUID id;

    @NotBlank(message = "Stage name is required")
    private String stageName;

    @NotNull(message = "Stage order is required")
    private Integer stageOrder;

    @Builder.Default
    private boolean enabled = true;
}
