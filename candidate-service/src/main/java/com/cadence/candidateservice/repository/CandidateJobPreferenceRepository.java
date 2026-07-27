package com.cadence.candidateservice.repository;

import com.cadence.candidateservice.entity.CandidateJobPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CandidateJobPreferenceRepository extends JpaRepository<CandidateJobPreference, UUID> {
    Optional<CandidateJobPreference> findByCandidateId(UUID candidateId);
}
