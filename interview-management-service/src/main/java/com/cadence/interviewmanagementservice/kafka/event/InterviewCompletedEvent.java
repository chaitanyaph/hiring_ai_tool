package com.cadence.interviewmanagementservice.kafka.event;

import com.cadence.interviewmanagementservice.constants.RecommendationType;
import com.cadence.interviewmanagementservice.constants.RoundType;
import lombok.*;

import java.util.UUID;

/** This service's own rich event (forward-scaffolded) -- carries the full feedback breakdown, unlike the aggregated bridge event published onto application-service's topic. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InterviewCompletedEvent {
    private UUID interviewId;
    private UUID applicationId;
    private UUID candidateId;
    private RoundType roundType;
    private Integer overallRating;
    private RecommendationType recommendation;
}
