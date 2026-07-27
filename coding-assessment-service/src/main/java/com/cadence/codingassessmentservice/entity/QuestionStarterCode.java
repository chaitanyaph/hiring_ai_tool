package com.cadence.codingassessmentservice.entity;

import com.cadence.codingassessmentservice.constants.ProgrammingLanguage;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "question_starter_code")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionStarterCode {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false, length = 20)
    private ProgrammingLanguage language;

    @Column(name = "code", nullable = false, columnDefinition = "TEXT")
    private String code;
}
