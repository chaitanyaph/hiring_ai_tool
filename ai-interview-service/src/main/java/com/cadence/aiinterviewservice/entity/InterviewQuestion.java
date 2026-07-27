package com.cadence.aiinterviewservice.entity;

import com.cadence.aiinterviewservice.constants.QuestionCategory;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "interview_question")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewQuestion {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "question_order", nullable = false)
    private int questionOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private QuestionCategory category;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "asked_at")
    private LocalDateTime askedAt;
}
