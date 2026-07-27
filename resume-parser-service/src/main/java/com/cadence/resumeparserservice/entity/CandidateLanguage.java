package com.cadence.resumeparserservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "candidate_language")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateLanguage {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "parsed_resume_id", nullable = false)
    private UUID parsedResumeId;

    @Column(name = "language_name", nullable = false, length = 100)
    private String languageName;

    @Column(name = "proficiency", length = 50)
    private String proficiency;
}
