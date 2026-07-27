package com.cadence.aiinterviewservice.entity;

import com.cadence.aiinterviewservice.constants.NoteType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "interview_feedback_note")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewFeedbackNote {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "note_type", nullable = false, length = 15)
    private NoteType noteType;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
