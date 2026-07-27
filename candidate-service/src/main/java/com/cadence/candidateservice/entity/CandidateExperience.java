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
@Table(name = "candidate_experience")
@SQLRestriction("is_deleted = false")
public class CandidateExperience extends BaseAuditEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "job_title", nullable = false, length = 150)
    private String jobTitle;

    @Column(name = "company_name", nullable = false, length = 150)
    private String companyName;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "currently_working", nullable = false)
    private boolean currentlyWorking;

    @Column(name = "achievements", columnDefinition = "LONGTEXT")
    private String achievements;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
