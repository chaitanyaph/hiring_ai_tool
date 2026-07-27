package com.cadence.resumeparserservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "candidate_experience")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateExperience {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "parsed_resume_id", nullable = false)
    private UUID parsedResumeId;

    @Column(name = "company_name", nullable = false, length = 150)
    private String companyName;

    @Column(name = "designation", length = 150)
    private String designation;

    @Column(name = "start_date", length = 20)
    private String startDate;

    @Column(name = "end_date", length = 20)
    private String endDate;

    // Deliberately named "current" not "isCurrent" -- Lombok generates an
    // asymmetric isCurrent()/setIsCurrent() pair for a field literally
    // named isCurrent, which MapStruct then can't auto-map (it sees two
    // different property names from the getter vs. the setter).
    @Column(name = "is_current", nullable = false)
    @Builder.Default
    private boolean current = false;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
