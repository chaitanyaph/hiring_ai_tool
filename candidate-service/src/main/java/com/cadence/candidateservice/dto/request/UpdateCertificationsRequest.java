package com.cadence.candidateservice.dto.request;

import jakarta.validation.Valid;
import lombok.*;

import java.util.List;

/** Wizard Step 7 -- full replace. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCertificationsRequest {
    @Valid
    private List<CertificationItemRequest> items;
}
