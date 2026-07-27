package com.cadence.interviewmanagementservice.dto.request;

import lombok.*;

/** Matches modal-cancel-interview (§A7): a single optional reason field. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancelInterviewRequest {
    private String cancelReason;
}
