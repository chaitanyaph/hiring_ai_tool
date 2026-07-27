package com.cadence.candidateservice.client.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompanyDto {
    private UUID id;
    private String companyName;
}
