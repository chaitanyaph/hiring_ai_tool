package com.cadence.resumeparserservice.entity;

import com.cadence.resumeparserservice.constants.NoteType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "resume_match_note")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeMatchNote {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "resume_match_id", nullable = false)
    private UUID resumeMatchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "note_type", nullable = false, length = 10)
    private NoteType noteType;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
