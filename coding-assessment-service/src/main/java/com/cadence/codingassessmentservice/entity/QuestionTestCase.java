package com.cadence.codingassessmentservice.entity;

import com.cadence.codingassessmentservice.constants.TestCaseVisibility;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "question_test_case")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionTestCase {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 10)
    private TestCaseVisibility visibility;

    @Column(name = "input_data", columnDefinition = "TEXT")
    private String inputData;

    @Column(name = "expected_output", nullable = false, columnDefinition = "TEXT")
    private String expectedOutput;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
