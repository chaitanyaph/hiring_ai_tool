package com.cadence.analyticsservice.feign.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CandidateDto {
    private UUID id;
    private String fullName;
    private String email;
}
