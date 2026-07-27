package com.cadence.codingassessmentservice.entity;

import com.cadence.codingassessmentservice.constants.NoteType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "ai_code_review_note")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiCodeReviewNote {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "ai_code_review_id", nullable = false)
    private UUID aiCodeReviewId;

    @Enumerated(EnumType.STRING)
    @Column(name = "note_type", nullable = false, length = 15)
    private NoteType noteType;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
