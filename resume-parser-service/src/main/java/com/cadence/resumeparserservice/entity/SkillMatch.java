package com.cadence.resumeparserservice.entity;

import com.cadence.resumeparserservice.constants.SkillCategory;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "skill_match")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillMatch {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "resume_match_id", nullable = false)
    private UUID resumeMatchId;

    @Column(name = "skill_name", nullable = false, length = 100)
    private String skillName;

    @Enumerated(EnumType.STRING)
    @Column(name = "skill_category", nullable = false, length = 30)
    private SkillCategory skillCategory;
}
