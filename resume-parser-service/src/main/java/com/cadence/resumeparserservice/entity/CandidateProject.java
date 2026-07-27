package com.cadence.resumeparserservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "candidate_project")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateProject {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "parsed_resume_id", nullable = false)
    private UUID parsedResumeId;

    @Column(name = "project_name", nullable = false, length = 200)
    private String projectName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "technologies", length = 500)
    private String technologies;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
