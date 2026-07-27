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
@Table(name = "candidate_projects")
@SQLRestriction("is_deleted = false")
public class CandidateProject extends BaseAuditEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "description", columnDefinition = "LONGTEXT")
    private String description;

    @Column(name = "project_url", length = 500)
    private String projectUrl;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
