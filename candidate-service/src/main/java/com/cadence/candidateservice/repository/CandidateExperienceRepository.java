package com.cadence.candidateservice.repository;

import com.cadence.candidateservice.entity.CandidateExperience;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CandidateExperienceRepository extends JpaRepository<CandidateExperience, UUID> {
    List<CandidateExperience> findAllByCandidateIdOrderByDisplayOrderAsc(UUID candidateId);
    long countByCandidateId(UUID candidateId);
}
