package com.cadence.candidateservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** 1:1 with Candidate -- wizard Step 10. */
@Entity
@Table(name = "candidate_portfolio_links")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidatePortfolioLink {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "candidate_id", nullable = false, unique = true)
    private UUID candidateId;

    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    @Column(name = "linkedin_url", length = 500)
    private String linkedinUrl;

    @Column(name = "github_url", length = 500)
    private String githubUrl;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
