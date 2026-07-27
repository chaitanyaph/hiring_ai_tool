package com.cadence.aiinterviewservice.repository;

import com.cadence.aiinterviewservice.constants.ShortlistDecision;
import com.cadence.aiinterviewservice.entity.CandidateShortlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CandidateShortlistRepository extends JpaRepository<CandidateShortlist, UUID> {

    Optional<CandidateShortlist> findByApplicationId(UUID applicationId);

    Page<CandidateShortlist> findAllByJobIdAndDecisionOrderByOverallMatchScoreDesc(UUID jobId, ShortlistDecision decision, Pageable pageable);

    List<CandidateShortlist> findAllByApplicationIdIn(List<UUID> applicationIds);

    long countByJobIdAndDecision(UUID jobId, ShortlistDecision decision);

    @Query("SELECT AVG(cs.overallMatchScore) FROM CandidateShortlist cs WHERE cs.jobId = :jobId AND cs.decision = 'SHORTLISTED'")
    Double findAvgShortlistedScoreByJob(@Param("jobId") UUID jobId);
}
