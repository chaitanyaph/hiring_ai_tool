package com.cadence.codingassessmentservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarkForReviewRequest {
    @NotNull
    private UUID questionId;

    private boolean marked;
}
