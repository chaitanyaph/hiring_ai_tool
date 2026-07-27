package com.cadence.resumeparserservice.entity;

import com.cadence.resumeparserservice.constants.SkillCategory;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "missing_skill")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MissingSkill {

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

    @Column(name = "is_required", nullable = false)
    @Builder.Default
    private boolean required = true;
}
