package com.cadence.interviewmanagementservice.dto.request;

import lombok.*;

/** Candidate-initiated "Request reschedule" (§A9) -- mirrors the Figma's mockToast-only behavior: logged, not auto-actioned. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestRescheduleRequest {
    private String reason;
}
