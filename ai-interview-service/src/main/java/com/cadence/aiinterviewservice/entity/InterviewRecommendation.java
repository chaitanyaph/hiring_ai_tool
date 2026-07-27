package com.cadence.aiinterviewservice.entity;

import com.cadence.aiinterviewservice.constants.HiringRecommendation;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "interview_recommendation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewRecommendation {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "session_id", nullable = false, unique = true)
    private UUID sessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "hiring_recommendation", nullable = false, length = 20)
    private HiringRecommendation hiringRecommendation;

    @Column(name = "interview_summary", columnDefinition = "TEXT")
    private String interviewSummary;

    @Column(name = "recruiter_summary", columnDefinition = "TEXT")
    private String recruiterSummary;
}
