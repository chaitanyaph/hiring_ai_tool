package com.cadence.codingassessmentservice.entity;

import com.cadence.codingassessmentservice.constants.Difficulty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/** The reusable, company-scoped question bank -- one question can belong to many assessments via assessment_question. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "question")
public class Question extends BaseAuditEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, length = 20)
    private Difficulty difficulty;

    @Column(name = "marks", nullable = false)
    private int marks;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "example_text", columnDefinition = "TEXT")
    private String exampleText;

    @Column(name = "constraints_text", columnDefinition = "TEXT")
    private String constraintsText;

    @Column(name = "tags", length = 300)
    private String tags;

    @Column(name = "allowed_languages", nullable = false, length = 200)
    private String allowedLanguages;
}
