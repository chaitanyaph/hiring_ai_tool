package com.cadence.candidateservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "candidate_languages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateLanguage {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "language_name", nullable = false, length = 60)
    private String languageName;
}
