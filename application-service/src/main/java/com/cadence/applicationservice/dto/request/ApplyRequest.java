package com.cadence.applicationservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

/** candidateId is never taken from the request body -- always the caller's own userId off the JWT. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplyRequest {
    @NotNull(message = "jobId is required")
    private UUID jobId;
}
