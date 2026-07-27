package com.cadence.resumeparserservice.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CertificationResponse {
    private String certificationName;
    private String issuingOrganization;
    private String issuedDate;
    private String expiryDate;
    private String credentialId;
}
