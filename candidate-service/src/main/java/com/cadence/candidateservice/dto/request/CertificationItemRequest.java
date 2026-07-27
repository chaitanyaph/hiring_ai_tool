package com.cadence.candidateservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CertificationItemRequest {
    private UUID id;

    @NotBlank(message = "Certification name is required")
    private String name;

    private String issuedBy;
    private LocalDate issueDate;
    private String credentialUrl;
}
