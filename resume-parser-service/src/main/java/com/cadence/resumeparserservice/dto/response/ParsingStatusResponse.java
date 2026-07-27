package com.cadence.resumeparserservice.dto.response;

import com.cadence.resumeparserservice.constants.ParsingStatus;
import lombok.*;

/** Backs the drawer's 4-step stepper -- lightweight, poll-friendly, separate from the full aggregate. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParsingStatusResponse {
    private ParsingStatus status;
    private int attemptCount;
    private String failureReason;
}
