package com.cadence.applicationservice.kafka.event;

import com.cadence.applicationservice.constant.InterviewType;
import lombok.*;

import java.util.UUID;

/**
 * Consumed -- one generic topic for every interview round (AI,
 * Technical, Manager, HR); interviewType tells the consumer which
 * status transition applies. Published by the (future) AI Interview
 * Service for AI rounds, and by the (future) Interview Scheduling
 * Service once a human panel submits feedback for the other rounds.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewCompletedEvent {
    private UUID applicationId;
    private InterviewType interviewType;
    private Integer score;
    private String feedback;
}
