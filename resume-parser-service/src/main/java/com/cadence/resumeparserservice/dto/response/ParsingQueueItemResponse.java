package com.cadence.resumeparserservice.dto.response;

import com.cadence.resumeparserservice.constants.ParsingStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Backs the #sec-parsing table. fullName/email are only populated once
 * status=PARSED (that's the only place this service gets that data
 * from) -- for QUEUED/PROCESSING/FAILED rows they're null, and the
 * frontend/gateway is expected to compose the candidate's display name
 * from Candidate Service, same as the "Job" column.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParsingQueueItemResponse {
    private UUID resumeId;
    private UUID candidateId;
    private String fullName;
    private String email;
    private ParsingStatus status;
    private Integer progressPercent;
    private LocalDateTime submittedAt;
}
