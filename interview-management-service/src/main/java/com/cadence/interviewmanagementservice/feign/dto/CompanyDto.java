package com.cadence.interviewmanagementservice.feign.dto;

import lombok.*;

import java.util.UUID;

/** Mirrors Company Service's minimal client-side projection -- just name resolution for interview detail views. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompanyDto {
    private UUID id;
    private String companyName;
}
