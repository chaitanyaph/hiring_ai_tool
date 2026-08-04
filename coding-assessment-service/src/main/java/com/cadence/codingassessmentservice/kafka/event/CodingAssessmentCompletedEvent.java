package com.cadence.codingassessmentservice.kafka.event;

import lombok.*;

import java.util.UUID;

/**
 * Consumed by application-service's ApplicationEventConsumer -- field names
 * must match that side's CodingAssessmentCompletedEvent exactly (Jackson
 * binds by property name). `passed` (score vs. the assessment's
 * passingScorePercent) drives whether the application advances to
 * TECHNICAL_INTERVIEW or is auto-rejected.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CodingAssessmentCompletedEvent {
    private UUID applicationId;
    private Integer score;
    private Boolean passed;
}
