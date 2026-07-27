package com.cadence.interviewmanagementservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/** Merges the suggested interview_panel + interviewer_assignment tables -- no reusable named-panel concept exists in the Figma. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "interview_panelist")
public class InterviewPanelist {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "interview_id", nullable = false)
    private UUID interviewId;

    @Column(name = "interviewer_id", nullable = false)
    private UUID interviewerId;

    @Column(name = "interviewer_role", length = 30)
    private String interviewerRole;

    @Column(name = "invited_at", nullable = false)
    @Builder.Default
    private LocalDateTime invitedAt = LocalDateTime.now();

    @Column(name = "feedback_submitted", nullable = false)
    @Builder.Default
    private boolean feedbackSubmitted = false;
}
