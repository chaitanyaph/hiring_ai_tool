package com.cadence.codingassessmentservice.entity;

import com.cadence.codingassessmentservice.constants.ProgrammingLanguage;
import com.cadence.codingassessmentservice.constants.SubmissionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** One row per submit attempt per question -- multiple attempts are allowed and all kept (Submission History shows every one), unlike Run Code which is unscored and goes to execution_log instead. */
@Entity
@Table(name = "submission")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Submission {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "candidate_assessment_id", nullable = false)
    private UUID candidateAssessmentId;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false, length = 20)
    private ProgrammingLanguage language;

    @Column(name = "code", nullable = false, columnDefinition = "LONGTEXT")
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SubmissionStatus status = SubmissionStatus.PENDING;

    @Column(name = "score")
    private Integer score;

    @Column(name = "test_cases_passed")
    private Integer testCasesPassed;

    @Column(name = "test_cases_total")
    private Integer testCasesTotal;

    @Column(name = "runtime_ms")
    private Integer runtimeMs;

    @Column(name = "memory_kb")
    private Integer memoryKb;

    @Column(name = "attempt_number", nullable = false)
    @Builder.Default
    private int attemptNumber = 1;

    @Column(name = "compile_output", columnDefinition = "TEXT")
    private String compileOutput;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @PrePersist
    protected void prePersist() {
        if (this.submittedAt == null) {
            this.submittedAt = LocalDateTime.now();
        }
    }
}
