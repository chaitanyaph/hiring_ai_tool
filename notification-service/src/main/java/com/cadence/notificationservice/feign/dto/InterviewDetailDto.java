package com.cadence.notificationservice.feign.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Mirrors interview-management-service's InterviewDetailResponse.
 * NOTE: this is an auth-protected endpoint, not an internal/machine-
 * to-machine one (confirmed by research -- interview-management-
 * service reserved an internal wildcard but never implemented a
 * controller under it). Calls via InterviewManagementServiceClient
 * will typically 401 without a service token; every call site wraps
 * this in a safe try/catch that degrades to the fields already on the
 * Kafka event payload instead.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InterviewDetailDto {
    private UUID id;
    private String candidateName;
    private String jobTitle;
    private String companyName;
    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    private String meetingLink;
}
