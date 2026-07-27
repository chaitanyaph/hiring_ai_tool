package com.cadence.resumeparserservice.repository;

import com.cadence.resumeparserservice.entity.AiRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AiRecommendationRepository extends JpaRepository<AiRecommendation, UUID> {
    Optional<AiRecommendation> findByResumeMatchId(UUID resumeMatchId);
    void deleteAllByResumeMatchId(UUID resumeMatchId);
}
