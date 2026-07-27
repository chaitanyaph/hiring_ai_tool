package com.cadence.applicationservice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/** Shared shape for all 4 internal score-update endpoints (resume/interview/coding/overall). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScoreUpdateRequest {
    @NotNull(message = "score is required")
    @Min(0)
    @Max(100)
    private Integer score;

    /** Which upstream service reported this score, e.g. "resume-matching-service". Informational only. */
    private String source;
}
