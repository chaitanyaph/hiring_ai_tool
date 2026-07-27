package com.cadence.candidateservice.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CertificationResponse {
    private UUID id;
    private String name;
    private String issuedBy;
    private LocalDate issueDate;
    private String credentialUrl;
}
