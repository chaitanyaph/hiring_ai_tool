package com.cadence.aiinterviewservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "interview_answer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewAnswer {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "question_id", nullable = false, unique = true)
    private UUID questionId;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "answer_text", columnDefinition = "TEXT")
    private String answerText;

    @Column(name = "response_time_seconds")
    private Integer responseTimeSeconds;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;
}
