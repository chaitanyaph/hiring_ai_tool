package com.cadence.codingassessmentservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Backs the Question Navigator's per-pill state (visited/marked-for-review), independent of whether a question has an accepted submission yet. */
@Entity
@Table(name = "candidate_question_progress")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateQuestionProgress {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "candidate_assessment_id", nullable = false)
    private UUID candidateAssessmentId;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "visited", nullable = false)
    @Builder.Default
    private boolean visited = false;

    @Column(name = "marked_for_review", nullable = false)
    @Builder.Default
    private boolean markedForReview = false;

    @Column(name = "last_visited_at")
    private LocalDateTime lastVisitedAt;
}
