package com.cadence.interviewmanagementservice.entity;

import com.cadence.interviewmanagementservice.constants.RoundType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/** Per-company round template (Module 1). Backs the schedule modal's round-type list once it becomes dynamic per company. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "interview_round")
public class InterviewRound extends BaseAuditEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private RoundType type;

    @Column(name = "round_order", nullable = false)
    @Builder.Default
    private int roundOrder = 0;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
