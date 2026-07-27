package com.cadence.candidateservice.repository;

import com.cadence.candidateservice.entity.CandidatePortfolioLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CandidatePortfolioLinkRepository extends JpaRepository<CandidatePortfolioLink, UUID> {
    Optional<CandidatePortfolioLink> findByCandidateId(UUID candidateId);
}
