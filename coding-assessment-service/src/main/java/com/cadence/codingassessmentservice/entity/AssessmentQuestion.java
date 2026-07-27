package com.cadence.codingassessmentservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "assessment_question")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentQuestion {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "assessment_id", nullable = false)
    private UUID assessmentId;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
