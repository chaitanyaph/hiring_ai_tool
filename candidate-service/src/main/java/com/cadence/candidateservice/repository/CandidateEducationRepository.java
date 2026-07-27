package com.cadence.candidateservice.repository;

import com.cadence.candidateservice.entity.CandidateEducation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CandidateEducationRepository extends JpaRepository<CandidateEducation, UUID> {
    List<CandidateEducation> findAllByCandidateIdOrderByDisplayOrderAsc(UUID candidateId);
    long countByCandidateId(UUID candidateId);
}
