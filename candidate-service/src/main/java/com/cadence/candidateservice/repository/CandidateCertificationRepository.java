package com.cadence.candidateservice.repository;

import com.cadence.candidateservice.entity.CandidateCertification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CandidateCertificationRepository extends JpaRepository<CandidateCertification, UUID> {
    List<CandidateCertification> findAllByCandidateIdOrderByDisplayOrderAsc(UUID candidateId);
    long countByCandidateId(UUID candidateId);
}
