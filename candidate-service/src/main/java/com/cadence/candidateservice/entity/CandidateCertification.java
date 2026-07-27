package com.cadence.candidateservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "candidate_certifications")
@SQLRestriction("is_deleted = false")
public class CandidateCertification extends BaseAuditEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "issued_by", length = 150)
    private String issuedBy;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "credential_url", length = 500)
    private String credentialUrl;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
