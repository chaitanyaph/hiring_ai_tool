package com.cadence.codingassessmentservice.feign.dto;

import lombok.*;

/** Mirrors Application Service's ScoreUpdateRequest. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScoreUpdateRequest {
    private Integer score;
    private String source;
}
