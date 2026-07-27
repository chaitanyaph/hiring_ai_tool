package com.cadence.resumeparserservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "candidate_education")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateEducation {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "parsed_resume_id", nullable = false)
    private UUID parsedResumeId;

    @Column(name = "institution_name", nullable = false, length = 200)
    private String institutionName;

    @Column(name = "degree", length = 150)
    private String degree;

    @Column(name = "field_of_study", length = 150)
    private String fieldOfStudy;

    @Column(name = "start_date", length = 20)
    private String startDate;

    @Column(name = "end_date", length = 20)
    private String endDate;

    @Column(name = "grade", length = 50)
    private String grade;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
