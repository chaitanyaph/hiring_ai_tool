package com.cadence.candidateservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "candidate_education")
@SQLRestriction("is_deleted = false")
public class CandidateEducation extends BaseAuditEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "degree", nullable = false, length = 150)
    private String degree;

    @Column(name = "institution", nullable = false, length = 200)
    private String institution;

    @Column(name = "start_year")
    private Integer startYear;

    @Column(name = "end_year")
    private Integer endYear;

    @Column(name = "grade", length = 30)
    private String grade;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
