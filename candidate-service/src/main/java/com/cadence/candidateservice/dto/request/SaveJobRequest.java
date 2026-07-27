package com.cadence.candidateservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaveJobRequest {
    @NotNull(message = "jobId is required")
    private UUID jobId;
}
