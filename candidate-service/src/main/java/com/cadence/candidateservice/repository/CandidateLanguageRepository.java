package com.cadence.candidateservice.repository;

import com.cadence.candidateservice.entity.CandidateLanguage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CandidateLanguageRepository extends JpaRepository<CandidateLanguage, UUID> {
    List<CandidateLanguage> findAllByCandidateId(UUID candidateId);
    long countByCandidateId(UUID candidateId);
    void deleteAllByCandidateId(UUID candidateId);
}
