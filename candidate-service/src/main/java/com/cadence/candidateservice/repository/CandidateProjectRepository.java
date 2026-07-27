package com.cadence.candidateservice.repository;

import com.cadence.candidateservice.entity.CandidateProject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CandidateProjectRepository extends JpaRepository<CandidateProject, UUID> {
    List<CandidateProject> findAllByCandidateIdOrderByDisplayOrderAsc(UUID candidateId);
    long countByCandidateId(UUID candidateId);
}
