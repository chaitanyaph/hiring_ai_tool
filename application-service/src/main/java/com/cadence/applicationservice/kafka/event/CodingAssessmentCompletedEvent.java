package com.cadence.applicationservice.kafka.event;

import lombok.*;

import java.util.UUID;

/**
 * Consumed -- published by coding-assessment-service. `passed` (score vs.
 * the assessment's passingScorePercent) determines whether the application
 * advances to TECHNICAL_INTERVIEW or is auto-rejected -- null (from an
 * older event or a manual "move to next stage" action taken before
 * evaluation ran) is treated as passing, preserving the old always-advance
 * behavior rather than silently rejecting on missing data.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodingAssessmentCompletedEvent {
    private UUID applicationId;
    private Integer score;
    private Boolean passed;
}
