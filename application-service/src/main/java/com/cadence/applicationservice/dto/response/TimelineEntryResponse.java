package com.cadence.applicationservice.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Backs GET /applications/{id}/timeline -- a single chronological feed
 * merging status and stage transitions, since a candidate/recruiter
 * wants one unified "what happened and when" view, not two separate lists.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimelineEntryResponse {
    private String type; // "STATUS" or "STAGE"
    private String from;
    private String to;
    private UUID changedBy;
    private LocalDateTime changedAt;
    private String reason;
}
