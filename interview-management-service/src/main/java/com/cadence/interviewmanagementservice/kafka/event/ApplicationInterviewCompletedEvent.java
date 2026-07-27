package com.cadence.interviewmanagementservice.kafka.event;

import com.cadence.interviewmanagementservice.constants.ApplicationInterviewType;
import lombok.*;

import java.util.UUID;

/**
 * EXACT shape application-service's ApplicationEventConsumer already
 * actively deserializes on interview.interview.completed -- do not add
 * or rename fields without checking that side first. score/feedback
 * are a single aggregate per event (not the multi-dimension breakdown
 * this service collects) -- aggregated in InterviewFeedbackServiceImpl.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApplicationInterviewCompletedEvent {
    private UUID applicationId;
    private ApplicationInterviewType interviewType;
    private Integer score;
    private String feedback;
}
