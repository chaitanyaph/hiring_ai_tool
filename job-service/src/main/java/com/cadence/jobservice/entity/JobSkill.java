package com.cadence.jobservice.entity;

import com.cadence.jobservice.constant.SkillType;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "job_skills")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobSkill implements Serializable {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "skill_name", nullable = false, length = 100)
    private String skillName;

    @Enumerated(EnumType.STRING)
    @Column(name = "skill_type", nullable = false, length = 20)
    private SkillType skillType;
}
