package com.cadence.resumeparserservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "candidate_certification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateCertification {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "parsed_resume_id", nullable = false)
    private UUID parsedResumeId;

    @Column(name = "certification_name", nullable = false, length = 200)
    private String certificationName;

    @Column(name = "issuing_organization", length = 200)
    private String issuingOrganization;

    @Column(name = "issued_date", length = 20)
    private String issuedDate;

    @Column(name = "expiry_date", length = 20)
    private String expiryDate;

    @Column(name = "credential_id", length = 100)
    private String credentialId;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
