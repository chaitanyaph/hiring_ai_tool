package com.cadence.candidateservice.repository;

import com.cadence.candidateservice.entity.SavedJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SavedJobRepository extends JpaRepository<SavedJob, UUID> {
    List<SavedJob> findAllByCandidateIdOrderBySavedAtDesc(UUID candidateId);
    Optional<SavedJob> findByCandidateIdAndJobId(UUID candidateId, UUID jobId);
    boolean existsByCandidateIdAndJobId(UUID candidateId, UUID jobId);
    long countByCandidateId(UUID candidateId);
    void deleteByCandidateIdAndJobId(UUID candidateId, UUID jobId);
}
