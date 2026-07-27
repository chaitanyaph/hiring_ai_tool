package com.cadence.codingassessmentservice.entity;

import com.cadence.codingassessmentservice.constants.ProgrammingLanguage;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** "Run Code" history -- unscored execution against sample/custom input, distinct from a graded submission. */
@Entity
@Table(name = "execution_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionLog {

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

    @Column(name = "custom_input", columnDefinition = "TEXT")
    private String customInput;

    @Column(name = "output", columnDefinition = "TEXT")
    private String output;

    @Column(name = "stderr", columnDefinition = "TEXT")
    private String stderr;

    @Column(name = "runtime_ms")
    private Integer runtimeMs;

    @Column(name = "memory_kb")
    private Integer memoryKb;

    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;

    @PrePersist
    protected void prePersist() {
        if (this.executedAt == null) {
            this.executedAt = LocalDateTime.now();
        }
    }
}
