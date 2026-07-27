package com.cadence.codingassessmentservice.repository;

import com.cadence.codingassessmentservice.entity.AiCodeReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AiCodeReviewRepository extends JpaRepository<AiCodeReview, UUID> {
    Optional<AiCodeReview> findBySubmissionId(UUID submissionId);
}
