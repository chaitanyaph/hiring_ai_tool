package com.cadence.jobservice.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PipelineStageResponse {
    private UUID id;
    private String stageName;
    private Integer stageOrder;
    private boolean enabled;
    private boolean systemDefault;
}
