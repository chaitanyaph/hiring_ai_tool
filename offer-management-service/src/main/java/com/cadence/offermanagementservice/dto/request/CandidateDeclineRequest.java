package com.cadence.offermanagementservice.dto.request;

import com.cadence.offermanagementservice.constants.DeclineReason;
import lombok.*;

/** Matches #modal-decline-offer -- reason is optional in the Figma. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateDeclineRequest {
    private DeclineReason reason;
}
