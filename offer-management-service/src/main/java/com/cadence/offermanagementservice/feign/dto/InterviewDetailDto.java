package com.cadence.offermanagementservice.feign.dto;

import lombok.*;

import java.util.UUID;

/** Auth-protected endpoint (no internal/M2M controller exists on interview-management-service) -- every call site wraps this in a safe try/catch, same posture as notification-service's own client to it. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InterviewDetailDto {
    private UUID id;
    private String candidateName;
    private String jobTitle;
    private String roundType;
    private Integer overallRating;
}
