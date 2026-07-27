package com.cadence.codingassessmentservice.feign.dto;

import lombok.*;

import java.util.UUID;

/** Mirrors Company Service's minimal client-side projection -- just name resolution for the assessment intro screen. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompanyDto {
    private UUID id;
    private String companyName;
}
