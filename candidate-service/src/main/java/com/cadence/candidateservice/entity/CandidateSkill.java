package com.cadence.candidateservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "candidate_skills")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateSkill {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "skill_name", nullable = false, length = 100)
    private String skillName;
}
