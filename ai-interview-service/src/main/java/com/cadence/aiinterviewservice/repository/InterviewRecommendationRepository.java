package com.cadence.aiinterviewservice.repository;

import com.cadence.aiinterviewservice.constants.HiringRecommendation;
import com.cadence.aiinterviewservice.entity.InterviewRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InterviewRecommendationRepository extends JpaRepository<InterviewRecommendation, UUID> {
    Optional<InterviewRecommendation> findBySessionId(UUID sessionId);
    void deleteBySessionId(UUID sessionId);
    long countBySessionIdInAndHiringRecommendation(java.util.List<UUID> sessionIds, HiringRecommendation hiringRecommendation);
}
