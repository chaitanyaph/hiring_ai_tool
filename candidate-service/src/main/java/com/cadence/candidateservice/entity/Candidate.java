package com.cadence.candidateservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * id is NOT auto-generated -- it is the same userId Auth Service issued
 * at registration, since a candidate profile is inherently 1:1 with a
 * candidate user account. Created explicitly by the service layer the
 * first time a candidate touches their profile (e.g. via the
 * USER_REGISTERED Kafka event, or lazily on first profile-wizard call).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "candidates")
@SQLRestriction("is_deleted = false")
public class Candidate extends BaseAuditEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "headline", length = 150)
    private String headline;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "current_company", length = 150)
    private String currentCompany;

    @Column(name = "notice_period_days")
    private Integer noticePeriodDays;

    @Column(name = "profile_photo_url", length = 500)
    private String profilePhotoUrl;

    @Column(name = "resume_url", length = 500)
    private String resumeUrl;

    @Column(name = "resume_filename")
    private String resumeFilename;

    @Column(name = "resume_parsed_at")
    private LocalDateTime resumeParsedAt;

    @Column(name = "ai_resume_score")
    private Integer aiResumeScore;

    @Column(name = "profile_completion_percent", nullable = false)
    @Builder.Default
    private Integer profileCompletionPercent = 0;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";
}
