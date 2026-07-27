package com.cadence.codingassessmentservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "submission_test_case")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionTestCase {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "submission_id", nullable = false)
    private UUID submissionId;

    @Column(name = "test_case_id", nullable = false)
    private UUID testCaseId;

    @Column(name = "passed", nullable = false)
    @Builder.Default
    private boolean passed = false;

    @Column(name = "actual_output", columnDefinition = "TEXT")
    private String actualOutput;

    @Column(name = "runtime_ms")
    private Integer runtimeMs;

    @Column(name = "memory_kb")
    private Integer memoryKb;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
