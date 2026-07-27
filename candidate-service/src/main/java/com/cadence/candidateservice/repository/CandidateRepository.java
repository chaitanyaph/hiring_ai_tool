package com.cadence.candidateservice.repository;

import com.cadence.candidateservice.entity.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CandidateRepository extends JpaRepository<Candidate, UUID> {
    Optional<Candidate> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
}
