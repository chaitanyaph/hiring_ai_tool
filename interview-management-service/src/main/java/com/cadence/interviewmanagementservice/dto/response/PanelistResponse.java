package com.cadence.interviewmanagementservice.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PanelistResponse {
    private UUID interviewerId;
    private String interviewerRole;
    private boolean feedbackSubmitted;
}
