package com.cadence.interviewmanagementservice.dto.response;

import com.cadence.interviewmanagementservice.constants.TimelineStage;
import com.cadence.interviewmanagementservice.constants.TimelineStatus;
import lombok.*;

import java.time.LocalDateTime;

/** Backs the "Hiring progress" / "Application progress" timeline (§A13). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateTimelineResponse {
    private TimelineStage stage;
    private TimelineStatus status;
    private LocalDateTime occurredAt;
    private Integer score;
    private String note;
}
