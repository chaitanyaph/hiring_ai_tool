package com.cadence.aiinterviewservice.feign.dto;

import lombok.*;

import java.util.UUID;

/** Mirrors Candidate Service's CandidateSummaryResponse -- only the fields this service needs. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CandidateDto {
    private UUID id;
    private String fullName;
    private String email;
    private String status;
}
